package com.daily_work_todo.controller;

import com.daily_work_todo.entity.PeriodicTask;
import com.daily_work_todo.service.PeriodicTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 周期任务规则表(PeriodicTask)表控制层
 */
@RestController
@RequestMapping("/periodicTask")
public class PeriodicTaskController {

    private static final String CYCLE_DAILY = "DAILY";
    private static final String CYCLE_WEEKLY = "WEEKLY";
    private static final String CYCLE_MONTHLY = "MONTHLY";
    private static final Set<String> CYCLE_TYPES = Set.of(CYCLE_DAILY, CYCLE_WEEKLY, CYCLE_MONTHLY);

    @Autowired
    private PeriodicTaskService periodicTaskService;

    /**
     * 新增周期任务规则
     */
    @PostMapping
    public boolean save(@RequestBody PeriodicTask rule) {
        normalize(rule);
        if (!valid(rule)) {
            return false;
        }
        Date now = new Date();
        rule.setPeriodicTaskId(UUID.randomUUID().toString().replace("-", ""));
        rule.setEnableStatus(1);
        rule.setDeleteStatus(0);
        rule.setLastGenerateDate(null);
        rule.setLastGenerateTaskId(null);
        rule.setLastFailDate(null);
        rule.setCreatedTime(now);
        rule.setUpdatedTime(now);
        boolean ok = periodicTaskService.save(rule);
        if (ok) {
            // 当天符合规则时立即生成
            periodicTaskService.generateDueTasks();
        }
        return ok;
    }

    /**
     * 根据主键更新周期任务规则（仅更新规则本身，不影响已生成任务）
     */
    @PutMapping
    public boolean updateById(@RequestBody PeriodicTask rule) {
        normalize(rule);
        if (rule.getPeriodicTaskId() == null || !valid(rule)) {
            return false;
        }
        PeriodicTask existing = periodicTaskService.getById(rule.getPeriodicTaskId());
        if (existing == null || existing.getDeleteStatus() == null || existing.getDeleteStatus() != 0) {
            return false;
        }
        boolean ok = periodicTaskService.lambdaUpdate()
                .eq(PeriodicTask::getPeriodicTaskId, rule.getPeriodicTaskId())
                .set(PeriodicTask::getTitle, rule.getTitle())
                .set(PeriodicTask::getContent, rule.getContent())
                .set(PeriodicTask::getTagId, rule.getTagId())
                .set(PeriodicTask::getPriorityId, rule.getPriorityId())
                .set(PeriodicTask::getCycleType, rule.getCycleType())
                .set(PeriodicTask::getWeeklyDay, rule.getWeeklyDay())
                .set(PeriodicTask::getMonthlyDay, rule.getMonthlyDay())
                .set(PeriodicTask::getStartDate, rule.getStartDate())
                .set(PeriodicTask::getUpdatedTime, new Date())
                .update();
        if (ok) {
            periodicTaskService.generateDueTasks();
        }
        return ok;
    }

    /**
     * 根据主键删除周期任务规则（软删除，已生成任务保留）
     */
    @DeleteMapping("/{id}")
    public boolean removeById(@PathVariable("id") String id) {
        return periodicTaskService.lambdaUpdate()
                .eq(PeriodicTask::getPeriodicTaskId, id)
                .set(PeriodicTask::getDeleteStatus, 1)
                .set(PeriodicTask::getUpdatedTime, new Date())
                .update();
    }

    /**
     * 根据主键查询周期任务规则
     */
    @GetMapping("/{id}")
    public PeriodicTask getById(@PathVariable("id") String id) {
        PeriodicTask rule = periodicTaskService.getById(id);
        if (rule != null) {
            periodicTaskService.fillNextPlanDate(rule);
        }
        return rule;
    }

    /**
     * 查询全部未删除的周期任务规则，按创建时间降序
     */
    @GetMapping("/list")
    public List<PeriodicTask> list() {
        List<PeriodicTask> rules = periodicTaskService.lambdaQuery()
                .eq(PeriodicTask::getDeleteStatus, 0)
                .orderByDesc(PeriodicTask::getCreatedTime)
                .list();
        rules.forEach(periodicTaskService::fillNextPlanDate);
        return rules;
    }

    /**
     * 启用或停用周期任务规则
     */
    @PutMapping("/{id}/enable/{enable}")
    public boolean setEnable(@PathVariable("id") String id, @PathVariable("enable") int enable) {
        boolean ok = periodicTaskService.lambdaUpdate()
                .eq(PeriodicTask::getPeriodicTaskId, id)
                .set(PeriodicTask::getEnableStatus, enable == 1 ? 1 : 0)
                .set(PeriodicTask::getUpdatedTime, new Date())
                .update();
        if (ok && enable == 1) {
            periodicTaskService.generateDueTasks();
        }
        return ok;
    }

    /**
     * 重试生成当期任务
     */
    @PostMapping("/{id}/retry")
    public boolean retry(@PathVariable("id") String id) {
        return periodicTaskService.retry(id);
    }

    /**
     * 检查并生成当天到期的周期任务，返回生成数量
     */
    @PostMapping("/generate")
    public int generate() {
        return periodicTaskService.generateDueTasks();
    }

    /**
     * 清理空值并裁剪首尾空格；不使用的周期条件字段置空
     */
    private void normalize(PeriodicTask rule) {
        if (rule.getTitle() != null) {
            rule.setTitle(rule.getTitle().trim());
        }
        if (rule.getContent() != null) {
            rule.setContent(rule.getContent().trim());
        }
        if (!CYCLE_WEEKLY.equals(rule.getCycleType())) {
            rule.setWeeklyDay(null);
        }
        if (!CYCLE_MONTHLY.equals(rule.getCycleType())) {
            rule.setMonthlyDay(null);
        }
    }

    private boolean valid(PeriodicTask rule) {
        if (rule.getTitle() == null || rule.getTitle().isEmpty()) {
            return false;
        }
        if (rule.getContent() == null || rule.getContent().isEmpty()) {
            return false;
        }
        if (rule.getStartDate() == null) {
            return false;
        }
        if (!CYCLE_TYPES.contains(rule.getCycleType())) {
            return false;
        }
        if (CYCLE_WEEKLY.equals(rule.getCycleType())
                && (rule.getWeeklyDay() == null || rule.getWeeklyDay() < 1 || rule.getWeeklyDay() > 7)) {
            return false;
        }
        if (CYCLE_MONTHLY.equals(rule.getCycleType())
                && (rule.getMonthlyDay() == null || rule.getMonthlyDay() < 1 || rule.getMonthlyDay() > 31)) {
            return false;
        }
        return true;
    }
}

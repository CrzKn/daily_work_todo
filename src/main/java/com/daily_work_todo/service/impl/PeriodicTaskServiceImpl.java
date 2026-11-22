package com.daily_work_todo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daily_work_todo.entity.PeriodicTask;
import com.daily_work_todo.entity.PersonalTask;
import com.daily_work_todo.entity.PtStatus;
import com.daily_work_todo.mapper.PeriodicTaskMapper;
import com.daily_work_todo.service.PeriodicTaskService;
import com.daily_work_todo.service.PersonalTaskService;
import com.daily_work_todo.service.PtStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 周期任务规则表(PeriodicTask)表服务实现类
 */
@Slf4j
@Service
public class PeriodicTaskServiceImpl extends ServiceImpl<PeriodicTaskMapper, PeriodicTask> implements PeriodicTaskService {

    private static final String CYCLE_DAILY = "DAILY";
    private static final String CYCLE_WEEKLY = "WEEKLY";
    private static final String CYCLE_MONTHLY = "MONTHLY";

    @Autowired
    private PersonalTaskService personalTaskService;

    @Autowired
    private PtStatusService ptStatusService;

    /**
     * 检查并生成当天到期的周期任务。
     * 使用 synchronized 避免并发请求重复生成。
     */
    @Override
    public synchronized int generateDueTasks() {
        LocalDate today = LocalDate.now();
        List<PeriodicTask> rules = lambdaQuery()
                .eq(PeriodicTask::getDeleteStatus, 0)
                .eq(PeriodicTask::getEnableStatus, 1)
                .list();
        int generated = 0;
        for (PeriodicTask rule : rules) {
            if (rule.getStartDate() == null || today.isBefore(rule.getStartDate())) {
                continue;
            }
            // 同一规则同一计划日期最多生成一次
            if (today.equals(rule.getLastGenerateDate())) {
                continue;
            }
            if (!matches(rule, today)) {
                continue;
            }
            if (generateTask(rule, today)) {
                generated++;
            }
        }
        return generated;
    }

    @Override
    public boolean retry(String id) {
        PeriodicTask rule = getById(id);
        if (rule == null || rule.getDeleteStatus() == null || rule.getDeleteStatus() != 0) {
            return false;
        }
        if (rule.getEnableStatus() == null || rule.getEnableStatus() != 1) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (rule.getStartDate() == null || today.isBefore(rule.getStartDate())) {
            return false;
        }
        if (today.equals(rule.getLastGenerateDate())) {
            return true;
        }
        if (!matches(rule, today)) {
            return false;
        }
        return generateTask(rule, today);
    }

    @Override
    public void fillNextPlanDate(PeriodicTask rule) {
        rule.setNextPlanDate(calcNextPlanDate(rule));
    }

    /**
     * 判断指定日期是否符合周期规则
     */
    private boolean matches(PeriodicTask rule, LocalDate date) {
        String cycleType = rule.getCycleType();
        if (CYCLE_DAILY.equals(cycleType)) {
            return true;
        }
        if (CYCLE_WEEKLY.equals(cycleType)) {
            return rule.getWeeklyDay() != null && date.getDayOfWeek().getValue() == rule.getWeeklyDay();
        }
        if (CYCLE_MONTHLY.equals(cycleType)) {
            return rule.getMonthlyDay() != null && date.getDayOfMonth() == rule.getMonthlyDay();
        }
        return false;
    }

    /**
     * 生成一条任务实例，并更新规则的生成记录
     */
    private boolean generateTask(PeriodicTask rule, LocalDate planDate) {
        try {
            PersonalTask task = new PersonalTask();
            task.setPtId(UUID.randomUUID().toString().replace("-", ""));
            task.setTitle(rule.getTitle());
            task.setContent(rule.getContent());
            task.setTagId(rule.getTagId());
            task.setPriorityId(rule.getPriorityId());
            Date now = new Date();
            task.setCreatedTme(now);
            task.setUpdatedTime(now);
            PtStatus firstStatus = ptStatusService.lambdaQuery()
                    .orderByAsc(PtStatus::getPtStatusOrder)
                    .last("LIMIT 1")
                    .one();
            if (firstStatus != null) {
                task.setPtStatusId(firstStatus.getPtStatusId());
            }
            personalTaskService.save(task);

            lambdaUpdate()
                    .eq(PeriodicTask::getPeriodicTaskId, rule.getPeriodicTaskId())
                    .set(PeriodicTask::getLastGenerateDate, planDate)
                    .set(PeriodicTask::getLastGenerateTaskId, task.getPtId())
                    .set(PeriodicTask::getLastFailDate, null)
                    .set(PeriodicTask::getUpdatedTime, new Date())
                    .update();

            rule.setLastGenerateDate(planDate);
            rule.setLastGenerateTaskId(task.getPtId());
            rule.setLastFailDate(null);
            return true;
        } catch (Exception e) {
            log.error("周期任务生成失败，规则ID：{}", rule.getPeriodicTaskId(), e);
            lambdaUpdate()
                    .eq(PeriodicTask::getPeriodicTaskId, rule.getPeriodicTaskId())
                    .set(PeriodicTask::getLastFailDate, planDate)
                    .set(PeriodicTask::getUpdatedTime, new Date())
                    .update();
            rule.setLastFailDate(planDate);
            return false;
        }
    }

    /**
     * 计算下次计划日期：不早于开始日期、不早于当天、符合周期条件、当天已生成时顺延到下一期
     */
    private LocalDate calcNextPlanDate(PeriodicTask rule) {
        if (rule.getEnableStatus() == null || rule.getEnableStatus() != 1) {
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalDate base = rule.getStartDate() == null ? today : rule.getStartDate();
        if (base.isBefore(today)) {
            base = today;
        }
        LocalDate candidate = nextOnOrAfter(rule, base);
        if (candidate == null) {
            return null;
        }
        if (candidate.equals(today) && today.equals(rule.getLastGenerateDate())) {
            candidate = nextAfter(rule, candidate);
        }
        return candidate;
    }

    /**
     * 从指定日期起，找第一个满足周期条件的日期
     */
    private LocalDate nextOnOrAfter(PeriodicTask rule, LocalDate base) {
        String cycleType = rule.getCycleType();
        if (CYCLE_DAILY.equals(cycleType)) {
            return base;
        }
        if (CYCLE_WEEKLY.equals(cycleType)) {
            if (rule.getWeeklyDay() == null) {
                return null;
            }
            int diff = (rule.getWeeklyDay() - base.getDayOfWeek().getValue() + 7) % 7;
            return base.plusDays(diff);
        }
        if (CYCLE_MONTHLY.equals(cycleType)) {
            if (rule.getMonthlyDay() == null) {
                return null;
            }
            return nextMonthly(base, rule.getMonthlyDay());
        }
        return null;
    }

    /**
     * 从指定日期之后，找下一期日期
     */
    private LocalDate nextAfter(PeriodicTask rule, LocalDate current) {
        String cycleType = rule.getCycleType();
        if (CYCLE_DAILY.equals(cycleType)) {
            return current.plusDays(1);
        }
        if (CYCLE_WEEKLY.equals(cycleType)) {
            return current.plusWeeks(1);
        }
        if (CYCLE_MONTHLY.equals(cycleType)) {
            return nextMonthly(current.plusDays(1), rule.getMonthlyDay());
        }
        return null;
    }

    /**
     * 按月查找日期；当月不存在该日（如 2 月 30 日）时跳过，不顺延到月末
     */
    private LocalDate nextMonthly(LocalDate base, Integer monthlyDay) {
        if (monthlyDay == null) {
            return null;
        }
        LocalDate cursor = base.withDayOfMonth(1);
        for (int i = 0; i < 24; i++) {
            if (monthlyDay <= cursor.lengthOfMonth()) {
                LocalDate date = cursor.withDayOfMonth(monthlyDay);
                if (!date.isBefore(base)) {
                    return date;
                }
            }
            cursor = cursor.plusMonths(1);
        }
        return null;
    }
}

package com.daily_work_todo.controller;

import com.daily_work_todo.entity.PersonalTask;
import com.daily_work_todo.entity.PtStatus;
import com.daily_work_todo.entity.Prioritys;
import com.daily_work_todo.service.PersonalTaskService;
import com.daily_work_todo.service.PrioritysService;
import com.daily_work_todo.service.PtStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 个人任务表(PersonalTask)表控制层
 *
 * @author makejava
 * @since 2026-11-07 20:33:13
 */
@RestController
@RequestMapping("/personalTask")
public class PersonalTaskController {

    @Autowired
    private PersonalTaskService personalTaskService;

    @Autowired
    private PtStatusService ptStatusService;

    @Autowired
    private PrioritysService prioritysService;

    /**
     * 新增个人任务
     */
    @PostMapping
    public boolean save(@RequestBody PersonalTask personalTask) {
        // 创建时间为当前时间，新建时不设置更新时间
        personalTask.setCreatedTme(new Date());
        personalTask.setPtId(UUID.randomUUID().toString());
        // 默认状态为排序字段最小的任务状态
        PtStatus firstStatus = ptStatusService.lambdaQuery()
                .orderByAsc(PtStatus::getPtStatusOrder)
                .last("LIMIT 1")
                .one();
        if (firstStatus != null) {
            personalTask.setPtStatusId(firstStatus.getPtStatusId());
            // 默认状态若已标记为历史，新建即写入归档时间
            if (Integer.valueOf(1).equals(firstStatus.getIsHistory())) {
                personalTask.setArchivedTime(new Date());
            }
        }
        return personalTaskService.save(personalTask);
    }

    /**
     * 根据主键删除个人任务
     */
    @DeleteMapping("/{id}")
    public boolean removeById(@PathVariable("id") String id) {
        return personalTaskService.removeById(id);
    }

    /**
     * 根据主键更新个人任务
     * 状态变更时维护归档时间：进入历史状态写入当前时间，离开历史状态置空，历史状态之间流转保持不变
     */
    @PutMapping
    public boolean updateById(@RequestBody PersonalTask personalTask) {
        personalTask.setUpdatedTime(new Date());
        String clearArchivedForId = null;
        if (personalTask.getPtId() != null && personalTask.getPtStatusId() != null) {
            PersonalTask old = personalTaskService.getById(personalTask.getPtId());
            if (old != null && !personalTask.getPtStatusId().equals(old.getPtStatusId())) {
                boolean newIsHistory = isHistoryStatus(personalTask.getPtStatusId());
                boolean oldIsHistory = old.getPtStatusId() != null && isHistoryStatus(old.getPtStatusId());
                if (newIsHistory && !oldIsHistory) {
                    // 首次进入历史状态：写入归档时间
                    personalTask.setArchivedTime(new Date());
                } else if (!newIsHistory && oldIsHistory) {
                    // 离开历史状态：需在 update 后显式置空（MyBatis-Plus 默认忽略 null 字段）
                    clearArchivedForId = personalTask.getPtId();
                }
                // 历史状态之间流转：不设置 archivedTime，updateById 忽略 null 以保留原值
            }
        }
        boolean ok = personalTaskService.updateById(personalTask);
        if (ok && clearArchivedForId != null) {
            personalTaskService.lambdaUpdate()
                    .eq(PersonalTask::getPtId, clearArchivedForId)
                    .set(PersonalTask::getArchivedTime, null)
                    .update();
        }
        return ok;
    }

    private boolean isHistoryStatus(String statusId) {
        if (statusId == null) {
            return false;
        }
        PtStatus status = ptStatusService.getById(statusId);
        return status != null && Integer.valueOf(1).equals(status.getIsHistory());
    }

    /**
     * 根据主键查询个人任务
     */
    @GetMapping("/{id}")
    public PersonalTask getById(@PathVariable("id") String id) {
        return personalTaskService.getById(id);
    }

    /**
     * 查询全部个人任务：先按优先级排序升序，再按创建时间降序，再按更新时间降序
     */
    @GetMapping("/list")
    public List<PersonalTask> list() {
        // 优先级字典：priorityId -> priorityOrder（数值越小越靠前）
        Map<String, Integer> priorityOrderMap = new HashMap<>();
        for (Prioritys p : prioritysService.list()) {
            priorityOrderMap.put(p.getPriorityId(), p.getPriorityOrder() == null ? Integer.MAX_VALUE : p.getPriorityOrder());
        }
        return personalTaskService.list().stream()
                .sorted(Comparator
                        // 1. 优先级排序升序；未设置优先级的放到最后
                        .comparingInt((PersonalTask t) -> priorityOrderMap.getOrDefault(t.getPriorityId(), Integer.MAX_VALUE))
                        // 2. 创建时间降序（先创建的排后面）
                        .thenComparing(PersonalTask::getCreatedTme, Comparator.nullsLast(Comparator.reverseOrder()))
                        // 3. 更新时间降序（最近更新的排前面）
                        .thenComparing(PersonalTask::getUpdatedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 查询历史任务：当前状态被标记为「计入历史回看」（is_history=1）且状态未被软删除的全部任务。
     * 排序：归档时间降序（空值排最后）→ 创建时间降序 → 更新时间降序。
     * 筛选、统计在前端完成，本期不分页。
     */
    @GetMapping("/history")
    public List<PersonalTask> history() {
        List<String> historyStatusIds = ptStatusService.lambdaQuery()
                .eq(PtStatus::getDeleteStatus, 0)
                .eq(PtStatus::getIsHistory, 1)
                .list()
                .stream()
                .map(PtStatus::getPtStatusId)
                .collect(Collectors.toList());
        if (historyStatusIds.isEmpty()) {
            return new ArrayList<>();
        }
        return personalTaskService.lambdaQuery()
                .in(PersonalTask::getPtStatusId, historyStatusIds)
                .list()
                .stream()
                .sorted(Comparator
                        .comparing(PersonalTask::getArchivedTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PersonalTask::getCreatedTme, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PersonalTask::getUpdatedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }
}

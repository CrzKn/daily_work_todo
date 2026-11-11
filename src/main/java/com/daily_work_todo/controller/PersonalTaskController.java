package com.daily_work_todo.controller;

import com.daily_work_todo.entity.PersonalTask;
import com.daily_work_todo.entity.PtStatus;
import com.daily_work_todo.entity.Prioritys;
import com.daily_work_todo.service.PersonalTaskService;
import com.daily_work_todo.service.PrioritysService;
import com.daily_work_todo.service.PtStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
     */
    @PutMapping
    public boolean updateById(@RequestBody PersonalTask personalTask) {
        personalTask.setUpdatedTime(new Date());
        return personalTaskService.updateById(personalTask);
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
}

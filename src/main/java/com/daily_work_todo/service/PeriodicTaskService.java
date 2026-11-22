package com.daily_work_todo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.daily_work_todo.entity.PeriodicTask;

/**
 * 周期任务规则表(PeriodicTask)表服务接口
 */
public interface PeriodicTaskService extends IService<PeriodicTask> {

    /**
     * 检查并生成当天到期的周期任务，返回本次生成的任务数量
     */
    int generateDueTasks();

    /**
     * 重试生成当天到期的任务
     */
    boolean retry(String id);

    /**
     * 计算并填充下次计划日期
     */
    void fillNextPlanDate(PeriodicTask rule);
}

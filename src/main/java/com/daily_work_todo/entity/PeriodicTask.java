package com.daily_work_todo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 周期任务规则表(PeriodicTask)表实体类
 */
@Data
@TableName("periodic_task")
public class PeriodicTask implements Serializable {

    private static final long serialVersionUID = 329215318719369586L;

    /**
     * 周期任务规则ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String periodicTaskId;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务内容
     */
    private String content;

    /**
     * 关联标签ID
     */
    private String tagId;

    /**
     * 关联优先级ID
     */
    private String priorityId;

    /**
     * 周期类型：DAILY 每日、WEEKLY 每周、MONTHLY 每月
     */
    private String cycleType;

    /**
     * 每周执行日：1=周一 ... 7=周日
     */
    private Integer weeklyDay;

    /**
     * 每月执行日：1-31
     */
    private Integer monthlyDay;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 启用状态：1 启用、0 停用
     */
    private Integer enableStatus;

    /**
     * 最近一次成功生成的计划日期
     */
    private LocalDate lastGenerateDate;

    /**
     * 最近一次生成的任务ID
     */
    private String lastGenerateTaskId;

    /**
     * 最近一次生成失败的计划日期
     */
    private LocalDate lastFailDate;

    /**
     * 软删除标记：0 未删除、1 已删除
     */
    private Integer deleteStatus;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;

    /**
     * 下次计划日期（查询时计算，不存储）
     */
    @TableField(exist = false)
    private LocalDate nextPlanDate;

}

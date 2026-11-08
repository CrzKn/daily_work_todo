package com.daily_work_todo.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务状态表(PtStatus)实体类
 *
 * @author makejava
 * @since 2026-11-07 22:45:31
 */
@Data
public class PtStatus implements Serializable {

    private static final long serialVersionUID = 329215318719369585L;
/**
     * 任务状态id
     */
    private String ptStatusId;
/**
     * 任务状态名称
     */
    private String ptStatusName;
/**
     * 排序字段
     */
    private Integer ptStatusOrder;

}


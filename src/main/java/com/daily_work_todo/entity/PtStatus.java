package com.daily_work_todo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务状态表(PtStatus)实体类
 *
 * @author makejava
 * @since 2026-11-07 22:45:31
 */
@Data
@TableName("pt_status")
public class PtStatus implements Serializable {

    private static final long serialVersionUID = 329215318719369585L;
/**
     * 任务状态id
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String ptStatusId;
/**
     * 任务状态名称
     */
    private String ptStatusName;
/**
     * 排序字段
     */
    private Integer ptStatusOrder;
/**
     * 删除状态
     */
    private Integer deleteStatus;

}


package com.daily_work_todo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.io.Serializable;

/**
 * 优先级表(Prioritys)表实体类
 *
 * @author makejava
 * @since 2026-11-07 22:44:41
 */
@Data
@TableName("prioritys")
public class Prioritys implements Serializable {

    private static final long serialVersionUID = 329215318719369585L;
//优先级id
    @TableId(type = IdType.ASSIGN_UUID)
    private String priorityId;
//优先级名称
    private String priorityName;
//排序字段
    private Integer priorityOrder;

}


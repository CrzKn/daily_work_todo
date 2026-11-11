package com.daily_work_todo.entity;


import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 个人任务表(PersonalTask)表实体类
 *
 * @author makejava
 * @since 2026-11-07 20:33:13
 */
@Data
@TableName("personal_task")
public class PersonalTask implements Serializable {

    private static final long serialVersionUID = 329215318719369585L;
//主键
    @TableId(type = IdType.ASSIGN_UUID)
    private String ptId;
//任务标题
    private String title;
//任务正文（纯文本）
    private String content;
//标签id
    private String tagId;
//创建时间
    private Date createdTme;
//更新时间（ALWAYS：插入时即使为 null 也显式写入，避免被数据库列默认值填充）
    @TableField(insertStrategy = FieldStrategy.ALWAYS)
    private Date updatedTime;
//优先级id
    private String priorityId;
//状态id
    private String ptStatusId;

}


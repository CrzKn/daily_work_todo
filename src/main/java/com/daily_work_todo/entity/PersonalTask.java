package com.daily_work_todo.entity;


import com.baomidou.mybatisplus.extension.activerecord.Model;
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
public class PersonalTask{
//主键
    private String ptId;
//任务正文（纯文本）
    private String content;
//标签id
    private String tagId;
//创建时间
    private Date createdTme;
//更新时间
    private Date updatedTime;
//优先级id
    private String priorityId;
//状态id
    private String ptStatusId;

}


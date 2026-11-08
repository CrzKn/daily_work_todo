package com.daily_work_todo.entity;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.io.Serializable;

/**
 * 标签表(Tags)表实体类
 *
 * @author makejava
 * @since 2026-11-07 22:42:23
 */
@Data
public class Tags implements Serializable {

    private static final long serialVersionUID = 329215318719369585L;
//标签id
    private String tagId;
//标签名称
    private String tagName;

}


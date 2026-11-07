package com.daily_work_todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daily_work_todo.entity.PersonalTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个人任务表(PersonalTask)表数据库访问层
 *
 * @author makejava
 * @since 2026-11-07 20:33:13
 */
@Mapper
public interface PersonalTaskMapper extends BaseMapper<PersonalTask> {

}

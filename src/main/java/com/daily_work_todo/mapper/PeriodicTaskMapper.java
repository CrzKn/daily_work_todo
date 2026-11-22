package com.daily_work_todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daily_work_todo.entity.PeriodicTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周期任务规则表(PeriodicTask)表数据库访问层
 */
@Mapper
public interface PeriodicTaskMapper extends BaseMapper<PeriodicTask> {

}

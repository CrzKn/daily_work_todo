package com.daily_work_todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daily_work_todo.entity.PtStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务状态表(PtStatus)表数据库访问层
 *
 * @author makejava
 * @since 2026-11-07 22:45:31
 */
@Mapper
public interface PtStatusMapper extends BaseMapper<PtStatus> {

}

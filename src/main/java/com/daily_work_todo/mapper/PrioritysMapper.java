package com.daily_work_todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daily_work_todo.entity.Prioritys;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优先级表(Prioritys)表数据库访问层
 *
 * @author makejava
 * @since 2026-11-07 22:44:41
 */
@Mapper
public interface PrioritysMapper extends BaseMapper<Prioritys> {

}

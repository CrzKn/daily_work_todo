package com.daily_work_todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daily_work_todo.entity.Tags;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签表(Tags)表数据库访问层
 *
 * @author makejava
 * @since 2026-11-07 22:42:23
 */
@Mapper
public interface TagsMapper extends BaseMapper<Tags> {

}

package com.daily_work_todo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daily_work_todo.entity.Tags;
import com.daily_work_todo.mapper.TagsMapper;
import com.daily_work_todo.service.TagsService;
import org.springframework.stereotype.Service;

/**
 * 标签表(Tags)表服务实现类
 *
 * @author makejava
 * @since 2026-11-07 22:42:23
 */
@Service
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {

}

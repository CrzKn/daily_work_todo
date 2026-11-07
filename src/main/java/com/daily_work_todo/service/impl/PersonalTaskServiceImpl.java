package com.daily_work_todo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daily_work_todo.entity.PersonalTask;
import com.daily_work_todo.mapper.PersonalTaskMapper;
import com.daily_work_todo.service.PersonalTaskService;
import org.springframework.stereotype.Service;

/**
 * 个人任务表(PersonalTask)表服务实现类
 *
 * @author makejava
 * @since 2026-11-07 20:33:13
 */
@Service
public class PersonalTaskServiceImpl extends ServiceImpl<PersonalTaskMapper, PersonalTask> implements PersonalTaskService {

}

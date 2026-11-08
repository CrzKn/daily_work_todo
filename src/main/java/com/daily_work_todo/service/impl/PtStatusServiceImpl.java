package com.daily_work_todo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.daily_work_todo.entity.PtStatus;
import com.daily_work_todo.mapper.PtStatusMapper;
import com.daily_work_todo.service.PtStatusService;
import org.springframework.stereotype.Service;

/**
 * 任务状态表(PtStatus)表服务实现类
 *
 * @author makejava
 * @since 2026-11-07 22:45:31
 */
@Service
public class PtStatusServiceImpl extends ServiceImpl<PtStatusMapper, PtStatus> implements PtStatusService {

}

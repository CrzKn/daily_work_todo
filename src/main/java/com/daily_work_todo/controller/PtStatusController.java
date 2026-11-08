package com.daily_work_todo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daily_work_todo.entity.PtStatus;
import com.daily_work_todo.service.PtStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务状态表(PtStatus)表控制层
 *
 * @author makejava
 * @since 2026-11-07 22:45:31
 */
@RestController
@RequestMapping("/ptStatus")
public class PtStatusController {

    @Autowired
    private PtStatusService ptStatusService;

    /**
     * 新增任务状态
     */
    @PostMapping
    public boolean save(@RequestBody PtStatus ptStatus) {
        return ptStatusService.save(ptStatus);
    }

    /**
     * 根据主键删除任务状态
     */
    @DeleteMapping("/{id}")
    public boolean removeById(@PathVariable("id") String id) {
        return ptStatusService.removeById(id);
    }

    /**
     * 根据主键更新任务状态
     */
    @PutMapping
    public boolean updateById(@RequestBody PtStatus ptStatus) {
        return ptStatusService.updateById(ptStatus);
    }

    /**
     * 根据主键查询任务状态
     */
    @GetMapping("/{id}")
    public PtStatus getById(@PathVariable("id") String id) {
        return ptStatusService.getById(id);
    }

    /**
     * 查询全部任务状态
     */
    @GetMapping("/list")
    public List<PtStatus> list() {
        return ptStatusService.list();
    }

    /**
     * 分页查询任务状态
     */
    @GetMapping("/page")
    public Page<PtStatus> page(@RequestParam(defaultValue = "1") long current,
                               @RequestParam(defaultValue = "10") long size) {
        return ptStatusService.page(new Page<>(current, size), new QueryWrapper<>());
    }
}

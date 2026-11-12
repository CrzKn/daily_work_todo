package com.daily_work_todo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daily_work_todo.entity.Prioritys;
import com.daily_work_todo.service.PrioritysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优先级表(Prioritys)表控制层
 *
 * @author makejava
 * @since 2026-11-07 22:44:41
 */
@RestController
@RequestMapping("/prioritys")
public class PrioritysController {

    @Autowired
    private PrioritysService prioritysService;

    /**
     * 新增优先级
     */
    @PostMapping
    public boolean save(@RequestBody Prioritys prioritys) {
        // 新增时删除状态默认为 0（未删除）
        prioritys.setDeleteStatus(0);
        return prioritysService.save(prioritys);
    }

    /**
     * 根据主键删除优先级（软删除：将 deleteStatus 置为 1）
     */
    @DeleteMapping("/{id}")
    public boolean removeById(@PathVariable("id") String id) {
        Prioritys prioritys = new Prioritys();
        prioritys.setPriorityId(id);
        prioritys.setDeleteStatus(1);
        return prioritysService.updateById(prioritys);
    }

    /**
     * 根据主键更新优先级
     */
    @PutMapping
    public boolean updateById(@RequestBody Prioritys prioritys) {
        return prioritysService.updateById(prioritys);
    }

    /**
     * 根据主键查询优先级
     */
    @GetMapping("/{id}")
    public Prioritys getById(@PathVariable("id") String id) {
        return prioritysService.getById(id);
    }

    /**
     * 查询全部优先级（仅查询 deleteStatus 为 0 的数据）
     */
    @GetMapping("/list")
    public List<Prioritys> list() {
        return prioritysService.lambdaQuery()
                .eq(Prioritys::getDeleteStatus, 0)
                .list();
    }
}

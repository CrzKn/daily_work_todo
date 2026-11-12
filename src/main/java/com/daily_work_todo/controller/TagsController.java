package com.daily_work_todo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daily_work_todo.entity.Tags;
import com.daily_work_todo.service.TagsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签表(Tags)表控制层
 *
 * @author makejava
 * @since 2026-11-07 22:42:23
 */
@RestController
@RequestMapping("/tags")
public class TagsController {

    @Autowired
    private TagsService tagsService;

    /**
     * 新增标签
     */
    @PostMapping
    public boolean save(@RequestBody Tags tags) {
        // 新增时删除状态默认为 0（未删除）
        tags.setDeleteStatus(0);
        return tagsService.save(tags);
    }

    /**
     * 根据主键删除标签（软删除：将 deleteStatus 置为 1）
     */
    @DeleteMapping("/{id}")
    public boolean removeById(@PathVariable("id") String id) {
        Tags tags = new Tags();
        tags.setTagId(id);
        tags.setDeleteStatus(1);
        return tagsService.updateById(tags);
    }

    /**
     * 根据主键更新标签
     */
    @PutMapping
    public boolean updateById(@RequestBody Tags tags) {
        return tagsService.updateById(tags);
    }

    /**
     * 根据主键查询标签
     */
    @GetMapping("/{id}")
    public Tags getById(@PathVariable("id") String id) {
        return tagsService.getById(id);
    }

    /**
     * 查询全部标签（仅查询 deleteStatus 为 0 的数据）
     */
    @GetMapping("/list")
    public List<Tags> list() {
        return tagsService.lambdaQuery()
                .eq(Tags::getDeleteStatus, 0)
                .list();
    }
}

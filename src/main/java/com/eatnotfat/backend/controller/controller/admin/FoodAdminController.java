package com.eatnotfat.backend.controller.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatnotfat.backend.entity.FoodStandard;
import com.eatnotfat.backend.service.FoodStandardService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/food")
public class FoodAdminController {

    @Autowired
    private FoodStandardService foodStandardService;

    /**
     * 获取食物列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getFoodList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size) {

        LambdaQueryWrapper<FoodStandard> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(FoodStandard::getName, keyword)
                    .or()
                    .like(FoodStandard::getAlias, keyword);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(FoodStandard::getCategory, category);
        }
        wrapper.orderByDesc(FoodStandard::getCreateTime);

        Page<FoodStandard> pageResult = foodStandardService.page(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        List<String> categories = foodStandardService.getCategories();
        return Result.success(categories);
    }

    /**
     * 新增食物
     */
    @PostMapping
    public Result<FoodStandard> addFood(@RequestBody FoodStandard food) {
        food.setId(null);
        food.setCreateTime(LocalDateTime.now());
        food.setUpdateTime(LocalDateTime.now());
        foodStandardService.save(food);
        return Result.success(food);
    }

    /**
     * 获取食物详情
     */
    @GetMapping("/{id}")
    public Result<FoodStandard> getFood(@PathVariable Long id) {
        FoodStandard food = foodStandardService.getById(id);
        return food != null ? Result.success(food) : Result.error("食物不存在");
    }

    /**
     * 编辑食物
     */
    @PutMapping("/{id}")
    public Result<FoodStandard> updateFood(@PathVariable Long id, @RequestBody FoodStandard food) {
        FoodStandard existing = foodStandardService.getById(id);
        if (existing == null) {
            return Result.error("食物不存在");
        }
        food.setId(id);
        food.setUpdateTime(LocalDateTime.now());
        foodStandardService.updateById(food);
        return Result.success(food);
    }

    /**
     * 删除食物
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFood(@PathVariable Long id) {
        foodStandardService.removeById(id);
        return Result.success(null);
    }

    /**
     * 启用/停用食物
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        FoodStandard food = foodStandardService.getById(id);
        if (food == null) {
            return Result.error("食物不存在");
        }
        food.setStatus(body.get("status"));
        foodStandardService.updateById(food);
        return Result.success(null);
    }
}
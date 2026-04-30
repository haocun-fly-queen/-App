package com.eatnotfat.backend.controller.controller;

import com.eatnotfat.backend.entity.FoodStandard;
import com.eatnotfat.backend.service.FoodStandardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food")
public class FoodStandardController {

    @Autowired
    private FoodStandardService foodStandardService;

    /**
     * 获取所有食物
     */
    @GetMapping("/list")
    public Map<String, Object> getAllFoods() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<FoodStandard> foods = foodStandardService.getAllFoods();
            result.put("code", 200);
            result.put("data", foods);
            result.put("total", foods.size());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 搜索食物
     */
    @GetMapping("/search")
    public Map<String, Object> searchFood(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<FoodStandard> foods = foodStandardService.searchFood(keyword);
            result.put("code", 200);
            result.put("data", foods);
            result.put("keyword", keyword);
            result.put("total", foods.size());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 按分类获取食物
     */
    @GetMapping("/category/{category}")
    public Map<String, Object> getFoodsByCategory(@PathVariable String category) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<FoodStandard> foods = foodStandardService.getFoodsByCategory(category);
            result.put("code", 200);
            result.put("data", foods);
            result.put("category", category);
            result.put("total", foods.size());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取食物分类列表
     */
    @GetMapping("/categories")
    public Map<String, Object> getCategories() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> categories = foodStandardService.getCategories();
            result.put("code", 200);
            result.put("data", categories);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取单个食物详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getFoodById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            FoodStandard food = foodStandardService.getById(id);
            if (food != null) {
                result.put("code", 200);
                result.put("data", food);
            } else {
                result.put("code", 404);
                result.put("message", "食物不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
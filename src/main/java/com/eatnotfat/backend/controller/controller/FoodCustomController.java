package com.eatnotfat.backend.controller.controller;

import com.eatnotfat.backend.entity.FoodCustom;
import com.eatnotfat.backend.service.FoodCustomService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food/custom")
public class FoodCustomController {

    @Autowired
    private FoodCustomService foodCustomService;

    /**
     * 获取用户的自定义食物列表
     */
    @GetMapping("/list")
    public Result<List<FoodCustom>> getList(@RequestParam Long userId) {
        try {
            List<FoodCustom> list = foodCustomService.getByUser(userId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加自定义食物
     */
    @PostMapping
    public Result<FoodCustom> add(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            String name = params.get("name").toString();
            BigDecimal calorie = new BigDecimal(params.get("caloriePer100g").toString());
            BigDecimal carbs = params.containsKey("carbsPer100g") ?
                    new BigDecimal(params.get("carbsPer100g").toString()) : BigDecimal.ZERO;
            BigDecimal protein = params.containsKey("proteinPer100g") ?
                    new BigDecimal(params.get("proteinPer100g").toString()) : BigDecimal.ZERO;
            BigDecimal fat = params.containsKey("fatPer100g") ?
                    new BigDecimal(params.get("fatPer100g").toString()) : BigDecimal.ZERO;

            FoodCustom food = foodCustomService.addFood(userId, name, calorie, carbs, protein, fat);
            return Result.success(food);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新自定义食物
     */
    @PutMapping("/{id}")
    public Result<FoodCustom> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            String name = params.get("name").toString();
            BigDecimal calorie = new BigDecimal(params.get("caloriePer100g").toString());
            BigDecimal carbs = params.containsKey("carbsPer100g") ?
                    new BigDecimal(params.get("carbsPer100g").toString()) : BigDecimal.ZERO;
            BigDecimal protein = params.containsKey("proteinPer100g") ?
                    new BigDecimal(params.get("proteinPer100g").toString()) : BigDecimal.ZERO;
            BigDecimal fat = params.containsKey("fatPer100g") ?
                    new BigDecimal(params.get("fatPer100g").toString()) : BigDecimal.ZERO;

            FoodCustom food = foodCustomService.updateFood(id, userId, name, calorie, carbs, protein, fat);
            return Result.success(food);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除自定义食物
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        try {
            foodCustomService.deleteFood(id, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
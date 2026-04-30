package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.FoodStandard;
import java.util.List;

public interface FoodStandardService extends IService<FoodStandard> {

    /**
     * 搜索食物
     */
    List<FoodStandard> searchFood(String keyword);

    /**
     * 按分类获取食物
     */
    List<FoodStandard> getFoodsByCategory(String category);

    /**
     * 获取所有食物
     */
    List<FoodStandard> getAllFoods();

    /**
     * 获取食物分类列表
     */
    List<String> getCategories();
}
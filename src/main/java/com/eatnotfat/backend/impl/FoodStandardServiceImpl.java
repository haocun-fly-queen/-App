package com.eatnotfat.backend.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.FoodStandard;
import com.eatnotfat.backend.mapper.FoodStandardMapper;
import com.eatnotfat.backend.service.FoodStandardService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FoodStandardServiceImpl extends ServiceImpl<FoodStandardMapper, FoodStandard> implements FoodStandardService {

    @Override
    public List<FoodStandard> searchFood(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return this.getAllFoods();
        }
        return this.baseMapper.searchByName(keyword);
    }

    @Override
    public List<FoodStandard> getFoodsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return this.getAllFoods();
        }
        return this.baseMapper.selectByCategory(category);
    }

    @Override
    public List<FoodStandard> getAllFoods() {
        return this.baseMapper.selectAllEnabled();
    }

    @Override
    public List<String> getCategories() {
        // 查询所有不重复的分类
        List<FoodStandard> foods = this.getAllFoods();
        return foods.stream()
                .map(FoodStandard::getCategory)
                .distinct()
                .filter(c -> c != null && !c.isEmpty())
                .toList();
    }
}
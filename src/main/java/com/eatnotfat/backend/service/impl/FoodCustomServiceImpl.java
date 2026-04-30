package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.FoodCustom;
import com.eatnotfat.backend.mapper.FoodCustomMapper;
import com.eatnotfat.backend.service.FoodCustomService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FoodCustomServiceImpl extends ServiceImpl<FoodCustomMapper, FoodCustom> implements FoodCustomService {

    @Override
    public List<FoodCustom> getByUser(Long userId) {
        return this.baseMapper.selectByUser(userId);
    }

    @Override
    public FoodCustom addFood(Long userId, String name, BigDecimal caloriePer100g,
                              BigDecimal carbsPer100g, BigDecimal proteinPer100g, BigDecimal fatPer100g) {
        FoodCustom food = new FoodCustom();
        food.setUserId(userId);
        food.setName(name);
        food.setCaloriePer100g(caloriePer100g);
        food.setCarbsPer100g(carbsPer100g);
        food.setProteinPer100g(proteinPer100g);
        food.setFatPer100g(fatPer100g);
        food.setStatus(1);
        food.setCreateTime(LocalDateTime.now());
        food.setUpdateTime(LocalDateTime.now());
        this.save(food);
        return food;
    }

    @Override
    public FoodCustom updateFood(Long id, Long userId, String name, BigDecimal caloriePer100g,
                                 BigDecimal carbsPer100g, BigDecimal proteinPer100g, BigDecimal fatPer100g) {
        FoodCustom food = this.getById(id);
        if (food == null || !food.getUserId().equals(userId)) {
            throw new RuntimeException("食物不存在或无权限");
        }
        food.setName(name);
        food.setCaloriePer100g(caloriePer100g);
        food.setCarbsPer100g(carbsPer100g);
        food.setProteinPer100g(proteinPer100g);
        food.setFatPer100g(fatPer100g);
        food.setUpdateTime(LocalDateTime.now());
        this.updateById(food);
        return food;
    }

    @Override
    public void deleteFood(Long id, Long userId) {
        FoodCustom food = this.getById(id);
        if (food == null || !food.getUserId().equals(userId)) {
            throw new RuntimeException("食物不存在或无权限");
        }
        food.setStatus(0);
        this.updateById(food);
    }
}
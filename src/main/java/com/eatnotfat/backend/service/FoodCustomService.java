package com.eatnotfat.backend.service;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.FoodCustom;
import java.util.List;

public interface FoodCustomService extends IService<FoodCustom> {

    List<FoodCustom> getByUser(Long userId);

    FoodCustom addFood(Long userId, String name, BigDecimal caloriePer100g,
                       BigDecimal carbsPer100g, BigDecimal proteinPer100g, BigDecimal fatPer100g);

    FoodCustom updateFood(Long id, Long userId, String name, BigDecimal caloriePer100g,
                          BigDecimal carbsPer100g, BigDecimal proteinPer100g, BigDecimal fatPer100g);

    void deleteFood(Long id, Long userId);
}
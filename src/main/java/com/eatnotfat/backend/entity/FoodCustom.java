package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eat_not_fat_food_custom")
public class FoodCustom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    @TableField("calorie_per_100g")
    private BigDecimal caloriePer100g;

    @TableField("carbs_per_100g")
    private BigDecimal carbsPer100g;

    @TableField("protein_per_100g")
    private BigDecimal proteinPer100g;

    @TableField("fat_per_100g")
    private BigDecimal fatPer100g;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
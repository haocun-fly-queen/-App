package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eat_not_fat_food")
public class FoodStandard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String alias;
    private String category;

    @TableField("calorie_per_100g")
    private BigDecimal caloriePer100g;

    @TableField("carbs_per_100g")
    private BigDecimal carbsPer100g;

    @TableField("protein_per_100g")
    private BigDecimal proteinPer100g;

    @TableField("fat_per_100g")
    private BigDecimal fatPer100g;

    @TableField("fiber_per_100g")
    private BigDecimal fiberPer100g;

    @TableField("sodium_per_100g")
    private BigDecimal sodiumPer100g;

    private String defaultUnit;
    private String imageUrl;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eat_not_fat_diet_item")
public class DietRecordItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Integer foodType;

    private Long foodId;

    private String foodName;

    private BigDecimal eatWeight;

    private BigDecimal calorie;

    private BigDecimal carbs;

    private BigDecimal protein;

    private BigDecimal fat;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
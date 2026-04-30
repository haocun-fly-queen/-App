package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eat_not_fat_diet_record")
public class DietRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer mealType;

    private LocalDateTime mealTime;

    private BigDecimal totalCalorie;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
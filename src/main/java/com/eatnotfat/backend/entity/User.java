package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eat_not_fat_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String wxOpenid;

    private String phone;
    private String username;

    private String passwordHash;

    private Integer exerciseLevel;

    private String nickname;

    private String avatarUrl;

    private Integer gender;

    private Integer age;

    private BigDecimal height;

    private BigDecimal currentWeight;

    private BigDecimal targetWeight;

    /**
     * 活动水平：1-久坐，2-轻度活动，3-中度活动，4-重度活动，5-极重度活动
     */
    private Integer activityLevel;

    private Integer goalType;

    private Integer dailyCalorieGoal;

    private Integer status;

private String email;
    /**
     * 过敏源
     */
    private String allergies;

    /**
     * 饮食偏好：none/vegetarian/halal/taboo
     */
    private String dietPreference;

    /**
     * 忌口详情
     */
    private String tabooDetail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
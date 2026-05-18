package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exercise_library")
public class ExerciseLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String category;

    private BigDecimal metValue;

    private Integer difficulty;

    private String suitableGoal;

    private String muscleGroups;

    private String icon;

    private String description;

    private Integer isEnabled;

    private LocalDateTime createTime;
}

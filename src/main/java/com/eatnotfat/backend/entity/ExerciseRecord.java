package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exercise_record")
public class ExerciseRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long exerciseId;

    private String exerciseName;

    private String category;

    private Integer durationMinutes;

    private BigDecimal caloriesBurned;

    private Integer intensity;

    private Integer feeling;

    private String remark;

    private LocalDateTime recordTime;

    private LocalDateTime createTime;
}

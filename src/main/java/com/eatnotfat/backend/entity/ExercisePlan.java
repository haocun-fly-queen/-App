package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exercise_plan")
public class ExercisePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate planDate;

    private String planContent;

    private Integer status;

    private Long actualRecordId;

    private LocalDateTime createTime;
}

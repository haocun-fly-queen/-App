package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("exercise_reminder")
public class ExerciseReminder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalTime remindTime;

    private String remindDays;

    private Integer isEnabled;

    private LocalDateTime createTime;
}

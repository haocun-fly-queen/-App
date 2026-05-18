package com.eatnotfat.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExerciseRecordDTO {



    private Long userId;
    private Long exerciseId;
    private String exerciseName;
    private String category;
    private Integer durationMinutes;
    private BigDecimal caloriesBurned;
    private Integer intensity;      // 1低 2中 3高
    private Integer feeling;        // 1轻松 2适中 3吃力
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;
}

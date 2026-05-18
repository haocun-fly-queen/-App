package com.eatnotfat.backend.dto;

import lombok.Data;

@Data
public class ExerciseReminderDTO {
    private Long userId;
    private String remindTime;   // "18:00"
    private String remindDays;   // "1,2,3,4,5"
    private Integer isEnabled;
}

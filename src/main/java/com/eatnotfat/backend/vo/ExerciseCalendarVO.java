package com.eatnotfat.backend.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExerciseCalendarVO {
    private int year;
    private int month;
    private int totalDays;          // 本月运动天数
    private List<DayRecord> days;

    @Data
    public static class DayRecord {
        private String date;        // "2026-05-07"
        private boolean hasExercise;
        private double totalCalories;
        private int recordCount;
    }
}

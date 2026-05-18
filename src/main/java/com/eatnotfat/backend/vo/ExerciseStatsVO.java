package com.eatnotfat.backend.vo;

import lombok.Data;

import java.util.List;

@Data
public class ExerciseStatsVO {
    private List<DailyExercise> dailyExercises;
    private int totalDays;          // 运动天数
    private int totalCalories;      // 总消耗
    private int avgDuration;        // 日均时长
    private int exerciseTypeCount;  // 运动类型数
    private List<CategoryRatio> categoryRatios;  // 运动类型分布
    private List<IntakeVsBurn> intakeVsBurn;     // 摄入vs消耗

    @Data
    public static class DailyExercise {
        private String date;
        private int calories;
        private int target;
        private int duration;
    }

    @Data
    public static class CategoryRatio {
        private String category;
        private String categoryName;
        private int count;
        private double percent;
    }

    @Data
    public static class IntakeVsBurn {
        private String date;
        private int intake;         // 饮食摄入
        private int burn;           // 运动消耗
        private int net;            // 净摄入 = 摄入 - 消耗
    }
}

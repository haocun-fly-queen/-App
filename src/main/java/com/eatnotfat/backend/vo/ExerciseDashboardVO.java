package com.eatnotfat.backend.vo;

import lombok.Data;

import java.util.List;

@Data
public class ExerciseDashboardVO {

    private TodayExercise todayExercise;
    private ExercisePlanVO todayPlan;
    private FatigueStatus fatigue;

    @Data
    public static class TodayExercise {
        private int targetCalories;      // 运动消耗目标（建议值）
        private int consumedCalories;    // 已消耗
        private int remainingCalories;   // 剩余
        private double progressPercent;
        private List<ExerciseBrief> records;
    }

    @Data
    public static class ExerciseBrief {
        private Long id;
        private String exerciseName;
        private String icon;
        private int durationMinutes;
        private double caloriesBurned;
        private Integer intensity;
        private String recordTime;
    }

    @Data
    public static class ExercisePlanVO {
        private List<PlanExercise> exercises;
        private int totalDuration;
        private int totalCalories;
        private String advice;
        private boolean generated;  // 是否已生成
    }

    @Data
    public static class PlanExercise {
        private String name;
        private String icon;
        private String category;
        private int duration;
        private int calories;
        private String intensity;
        private String reason;
    }

    @Data
    public static class FatigueStatus {
        private int consecutiveDays;
        private boolean needRest;
        private String statusText;
        private String suggestion;
    }
}

package com.eatnotfat.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExercisePlanRequest {
    private Long userId;
    private UserProfile profile;
    private WeeklyDietData weeklyDiet;
    private WeeklyExerciseData weeklyExercise;
    private FatigueData fatigue;

    @Data
    public static class UserProfile {
        private Integer gender;            // 0未设置 1男 2女
        private String genderLabel;        // "男"/"女"/"未设置"
        private Integer age;
        private Double height;             // cm
        private Double weight;             // 当前体重 kg
        private Double targetWeight;       // 目标体重 kg
        private Double weightDiff;         // 当前-目标（正=需减，负=需增）
        private Integer goalType;          // 1减脂 2增肌 3维持
        private String goalLabel;          // "减脂"/"增肌"/"维持"
        private Integer activityLevel;     // 1-5
        private String activityLabel;      // "久坐"/"轻度活动"等
        private Integer exerciseLevel;     // 1新手 2进阶 3高级
        private String exerciseLevelLabel; // "新手"/"进阶"/"高级"
        private Double bmr;                // 基础代谢
        private Double tdee;               // 每日总消耗
        private Double targetCalorie;      // 每日目标摄入
        private Double exerciseCalorieGoal;// 建议运动消耗(TDEE×0.12)
    }

    @Data
    public static class WeeklyDietData {
        private int avgCalories;        // 近7日日均摄入
        private int targetCalories;     // 每日目标
        private int surplusDays;        // 热量超标天数
        private int totalSurplus;       // 累计盈余热量
        private List<DailyIntake> dailyIntakes;
    }

    @Data
    public static class DailyIntake {
        private String date;
        private int calories;
    }

    @Data
    public static class WeeklyExerciseData {
        private int activeDays;         // 运动天数
        private int totalBurned;        // 总消耗
        private int avgDuration;        // 平均时长
        private List<String> recentTypes; // 最近做的运动类型
    }

    @Data
    public static class FatigueData {
        private int consecutiveDays;    // 连续运动天数
        private boolean needRest;       // 是否需要休息
    }
}

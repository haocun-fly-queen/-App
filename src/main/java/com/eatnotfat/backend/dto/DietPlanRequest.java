package com.eatnotfat.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class DietPlanRequest {
    private Long userId;
    private UserProfile profile;        // 用户基础信息
    private DietRestrictions restrictions;  // 饮食限制
    private CalorieStatus calorieStatus;    // 热量状态
    private List<RecordedMeal> recordedMeals; // 已记录餐次
    private TargetMeal targetMeal;          // 目标餐次

    @Data
    public static class UserProfile {
        private Integer gender;
        private Integer age;
        private Double height;
        private Double weight;
        private Double targetWeight;
        private Integer activityLevel;
        private Integer goalType; // 1减脂 2增肌 3保持
        private Double bmr;
    }

    @Data
    public static class DietRestrictions {
        private String dietPreference; // vegetarian/halal/taboo/none
        private String tabooDetail;
        private List<String> allergies;
    }

    @Data
    public static class CalorieStatus {
        private Integer target;
        private Integer consumed;
        private Integer remaining;
        private Double progressPercent;
    }

    @Data
    public static class RecordedMeal {
        private Integer type;
        private String typeName;
        private String foods;
        private Integer calories;
        private Object nutrition;
    }

    @Data
    public static class TargetMeal {
        private Integer type;
        private String name;
    }
}
package com.eatnotfat.backend.vo;

import lombok.Data;
import java.util.List;

@Data
public class DietPlanResult {
    private String mealType;           // 餐次名称
    private Integer totalCalories;   // 总热量
    private List<FoodItem> foods;      // 食物列表
    private Nutrition nutrition;       // 营养占比
    private String reason;             // 推荐理由

    @Data
    public static class FoodItem {
        private String name;
        private String amount;
        private Integer calories;
        private String image;
    }

    @Data
    public static class Nutrition {
        private Integer carbs;
        private Integer protein;
        private Integer fat;
    }
}
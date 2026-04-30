package com.eatnotfat.backend.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RecognizeResult {
    private List<FoodItem> foods;
    private Double confidence;

    @Data
    public static class FoodItem {
        private String name;
        private BigDecimal calorie;  // 每100g热量
        private BigDecimal weight;   // 估算重量(g)
        private Double confidence;
        private Long foodId;          // 匹配的食物库ID
        private BigDecimal carbs;     // 每100g碳水
        private BigDecimal protein;   // 每100g蛋白质
        private BigDecimal fat;       // 每100g脂肪
    }
}
package com.eatnotfat.backend.vo;

import java.util.List;

public class DailyPlanResult {

    private List<MealPlan> meals;
    private DailySummary dailySummary;

    public static class MealPlan {
        private String mealType;
        private Integer mealTypeCode;   // 1=早 2=午 3=晚 4=加餐
        private Integer totalCalories;
        private List<FoodItem> foods;
        private Nutrition nutrition;
        private String reason;
        private Boolean recorded = false; // 前端标记是否已记录

        // getter/setter
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
        public Integer getMealTypeCode() { return mealTypeCode; }
        public void setMealTypeCode(Integer mealTypeCode) { this.mealTypeCode = mealTypeCode; }
        public Integer getTotalCalories() { return totalCalories; }
        public void setTotalCalories(Integer totalCalories) { this.totalCalories = totalCalories; }
        public List<FoodItem> getFoods() { return foods; }
        public void setFoods(List<FoodItem> foods) { this.foods = foods; }
        public Nutrition getNutrition() { return nutrition; }
        public void setNutrition(Nutrition nutrition) { this.nutrition = nutrition; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Boolean getRecorded() { return recorded; }
        public void setRecorded(Boolean recorded) { this.recorded = recorded; }
    }

    public static class FoodItem {
        private String name;
        private String amount;
        private Integer calories;
        private Integer carbs;      // 新增：碳水克数
        private Integer protein;    // 新增：蛋白质克数
        private Integer fat;        // 新增：脂肪克数
        private String image;

        // getter/setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }
        public Integer getCarbs() { return carbs; }
        public void setCarbs(Integer carbs) { this.carbs = carbs; }
        public Integer getProtein() { return protein; }
        public void setProtein(Integer protein) { this.protein = protein; }
        public Integer getFat() { return fat; }
        public void setFat(Integer fat) { this.fat = fat; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }

    public static class Nutrition {
        private Integer carbs;
        private Integer protein;
        private Integer fat;

        public Integer getCarbs() { return carbs; }
        public void setCarbs(Integer carbs) { this.carbs = carbs; }
        public Integer getProtein() { return protein; }
        public void setProtein(Integer protein) { this.protein = protein; }
        public Integer getFat() { return fat; }
        public void setFat(Integer fat) { this.fat = fat; }
    }

    public static class DailySummary {
        private Integer totalCalories;
        private Integer totalCarbs;
        private Integer totalProtein;
        private Integer totalFat;
        private String advice;

        public Integer getTotalCalories() { return totalCalories; }
        public void setTotalCalories(Integer totalCalories) { this.totalCalories = totalCalories; }
        public Integer getTotalCarbs() { return totalCarbs; }
        public void setTotalCarbs(Integer totalCarbs) { this.totalCarbs = totalCarbs; }
        public Integer getTotalProtein() { return totalProtein; }
        public void setTotalProtein(Integer totalProtein) { this.totalProtein = totalProtein; }
        public Integer getTotalFat() { return totalFat; }
        public void setTotalFat(Integer totalFat) { this.totalFat = totalFat; }
        public String getAdvice() { return advice; }
        public void setAdvice(String advice) { this.advice = advice; }
    }

    public List<MealPlan> getMeals() { return meals; }
    public void setMeals(List<MealPlan> meals) { this.meals = meals; }
    public DailySummary getDailySummary() { return dailySummary; }
    public void setDailySummary(DailySummary dailySummary) { this.dailySummary = dailySummary; }
}
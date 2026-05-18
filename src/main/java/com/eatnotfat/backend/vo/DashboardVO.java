package com.eatnotfat.backend.vo;

import java.util.List;

public class DashboardVO {

    private TodaySummary todaySummary;
    private NutritionStandard nutritionStandard;
    private PeriodStats weeklyStats;
    private PeriodStats monthlyStats;
    private List<Alert> alerts;

    // --- 今日摘要 ---
    public static class TodaySummary {
        private Integer targetCalories;
        private Integer consumedCalories;
        private Integer remainingCalories;
        private Double progressPercent;
        private List<MealBrief> meals;
        private MacroDetail macros;

        public Integer getTargetCalories() { return targetCalories; }
        public void setTargetCalories(Integer targetCalories) { this.targetCalories = targetCalories; }
        public Integer getConsumedCalories() { return consumedCalories; }
        public void setConsumedCalories(Integer consumedCalories) { this.consumedCalories = consumedCalories; }
        public Integer getRemainingCalories() { return remainingCalories; }
        public void setRemainingCalories(Integer remainingCalories) { this.remainingCalories = remainingCalories; }
        public Double getProgressPercent() { return progressPercent; }
        public void setProgressPercent(Double progressPercent) { this.progressPercent = progressPercent; }
        public List<MealBrief> getMeals() { return meals; }
        public void setMeals(List<MealBrief> meals) { this.meals = meals; }
        public MacroDetail getMacros() { return macros; }
        public void setMacros(MacroDetail macros) { this.macros = macros; }
    }

    public static class MealBrief {
        private Integer type;
        private String typeName;
        private Integer calories;
        private String foods;

        public Integer getType() { return type; }
        public void setType(Integer type) { this.type = type; }
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }
        public String getFoods() { return foods; }
        public void setFoods(String foods) { this.foods = foods; }
    }

    public static class MacroDetail {
        private MacroItem carbs;
        private MacroItem protein;
        private MacroItem fat;

        public MacroItem getCarbs() { return carbs; }
        public void setCarbs(MacroItem carbs) { this.carbs = carbs; }
        public MacroItem getProtein() { return protein; }
        public void setProtein(MacroItem protein) { this.protein = protein; }
        public MacroItem getFat() { return fat; }
        public void setFat(MacroItem fat) { this.fat = fat; }
    }

    public static class MacroItem {
        private Double actual;
        private Double target;
        private String unit;

        public MacroItem() {}
        public MacroItem(Double actual, Double target, String unit) {
            this.actual = actual; this.target = target; this.unit = unit;
        }
        public Double getActual() { return actual; }
        public void setActual(Double actual) { this.actual = actual; }
        public Double getTarget() { return target; }
        public void setTarget(Double target) { this.target = target; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    // --- 营养标准 ---
    public static class NutritionStandard {
        private Integer calories;
        private Integer carbsPercent;
        private Integer proteinPercent;
        private Integer fatPercent;
        private Double carbsGram;
        private Double proteinGram;
        private Double fatGram;
        private String description;

        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }
        public Integer getCarbsPercent() { return carbsPercent; }
        public void setCarbsPercent(Integer carbsPercent) { this.carbsPercent = carbsPercent; }
        public Integer getProteinPercent() { return proteinPercent; }
        public void setProteinPercent(Integer proteinPercent) { this.proteinPercent = proteinPercent; }
        public Integer getFatPercent() { return fatPercent; }
        public void setFatPercent(Integer fatPercent) { this.fatPercent = fatPercent; }
        public Double getCarbsGram() { return carbsGram; }
        public void setCarbsGram(Double carbsGram) { this.carbsGram = carbsGram; }
        public Double getProteinGram() { return proteinGram; }
        public void setProteinGram(Double proteinGram) { this.proteinGram = proteinGram; }
        public Double getFatGram() { return fatGram; }
        public void setFatGram(Double fatGram) { this.fatGram = fatGram; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // --- 周期统计 ---
    public static class PeriodStats {
        private List<DailyCalorie> dailyCalories;
        private Integer avgCalories;
        private Double mealRegularRate;
        private Integer foodDiversityScore;
        private Integer calorieFluctuation;
        private List<WeightPoint> weightTrend;

        public List<DailyCalorie> getDailyCalories() { return dailyCalories; }
        public void setDailyCalories(List<DailyCalorie> dailyCalories) { this.dailyCalories = dailyCalories; }
        public Integer getAvgCalories() { return avgCalories; }
        public void setAvgCalories(Integer avgCalories) { this.avgCalories = avgCalories; }
        public Double getMealRegularRate() { return mealRegularRate; }
        public void setMealRegularRate(Double mealRegularRate) { this.mealRegularRate = mealRegularRate; }
        public Integer getFoodDiversityScore() { return foodDiversityScore; }
        public void setFoodDiversityScore(Integer foodDiversityScore) { this.foodDiversityScore = foodDiversityScore; }
        public Integer getCalorieFluctuation() { return calorieFluctuation; }
        public void setCalorieFluctuation(Integer calorieFluctuation) { this.calorieFluctuation = calorieFluctuation; }
        public List<WeightPoint> getWeightTrend() { return weightTrend; }
        public void setWeightTrend(List<WeightPoint> weightTrend) { this.weightTrend = weightTrend; }
    }

    public static class DailyCalorie {
        private String date;
        private Integer calories;
        private Integer target;

        public DailyCalorie() {}
        public DailyCalorie(String date, Integer calories, Integer target) {
            this.date = date; this.calories = calories; this.target = target;
        }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }
        public Integer getTarget() { return target; }
        public void setTarget(Integer target) { this.target = target; }
    }

    public static class WeightPoint {
        private String date;
        private Double weight;

        public WeightPoint() {}
        public WeightPoint(String date, Double weight) {
            this.date = date; this.weight = weight;
        }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
    }

    // --- 异常提醒 ---
    public static class Alert {
        private String type;
        private String level;
        private String title;
        private String message;
        private String suggestion;

        public Alert() {}
        public Alert(String type, String level, String title, String message, String suggestion) {
            this.type = type; this.level = level; this.title = title;
            this.message = message; this.suggestion = suggestion;
        }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }

    public TodaySummary getTodaySummary() { return todaySummary; }
    public void setTodaySummary(TodaySummary todaySummary) { this.todaySummary = todaySummary; }
    public NutritionStandard getNutritionStandard() { return nutritionStandard; }
    public void setNutritionStandard(NutritionStandard nutritionStandard) { this.nutritionStandard = nutritionStandard; }
    public PeriodStats getWeeklyStats() { return weeklyStats; }
    public void setWeeklyStats(PeriodStats weeklyStats) { this.weeklyStats = weeklyStats; }
    public PeriodStats getMonthlyStats() { return monthlyStats; }
    public void setMonthlyStats(PeriodStats monthlyStats) { this.monthlyStats = monthlyStats; }
    public List<Alert> getAlerts() { return alerts; }
    public void setAlerts(List<Alert> alerts) { this.alerts = alerts; }
}

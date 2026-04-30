package com.eatnotfat.backend.service;

import com.eatnotfat.backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class DietPlanService {

    @Autowired
    private UserService userService;

    @Autowired
    private DietRecordService dietRecordService;

    /**
     * 获取用户饮食规划
     */
    public Map<String, Object> getDietPlan(Long userId) {
        Map<String, Object> plan = new HashMap<>();

        // 获取用户信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 获取每日热量目标
        int calorieGoal = user.getDailyCalorieGoal() != null ? user.getDailyCalorieGoal() : 2000;

        // 获取今日已摄入热量
        int todayIntake = dietRecordService.getDailyTotalCalories(userId, java.time.LocalDate.now());

        // 剩余热量
        int remainingCalories = Math.max(0, calorieGoal - todayIntake);

        // 计算三餐分配（早餐30%，午餐40%，晚餐30%，加餐可从中分配）
        int breakfastGoal = (int) Math.round(calorieGoal * 0.3);
        int lunchGoal = (int) Math.round(calorieGoal * 0.4);
        int dinnerGoal = (int) Math.round(calorieGoal * 0.3);

        // 营养素建议（碳水50%，蛋白质25%，脂肪25%）
        int carbsGoal = (int) Math.round(calorieGoal * 0.5 / 4);      // 4kcal/g
        int proteinGoal = (int) Math.round(calorieGoal * 0.25 / 4);   // 4kcal/g
        int fatGoal = (int) Math.round(calorieGoal * 0.25 / 9);       // 9kcal/g

        // 根据目标类型调整
        if (user.getGoalType() != null) {
            if (user.getGoalType() == 1) { // 减脂
                carbsGoal = (int) Math.round(carbsGoal * 0.9);
                proteinGoal = (int) Math.round(proteinGoal * 1.1);
                fatGoal = (int) Math.round(fatGoal * 0.9);
            } else if (user.getGoalType() == 2) { // 增肌
                carbsGoal = (int) Math.round(carbsGoal * 1.1);
                proteinGoal = (int) Math.round(proteinGoal * 1.2);
            }
        }

        // 食物推荐
        Map<String, Object> recommendations = getFoodRecommendations(user);

        // 饮食提醒
        String advice = getDietAdvice(user, todayIntake, calorieGoal);

        plan.put("calorieGoal", calorieGoal);
        plan.put("todayIntake", todayIntake);
        plan.put("remainingCalories", remainingCalories);
        plan.put("progressPercent", Math.min(100, (int) ((double) todayIntake / calorieGoal * 100)));
        plan.put("breakfastGoal", breakfastGoal);
        plan.put("lunchGoal", lunchGoal);
        plan.put("dinnerGoal", dinnerGoal);
        plan.put("carbsGoal", carbsGoal);
        plan.put("proteinGoal", proteinGoal);
        plan.put("fatGoal", fatGoal);
        plan.put("recommendations", recommendations);
        plan.put("advice", advice);
        plan.put("goalType", user.getGoalType());

        return plan;
    }

    /**
     * 获取食物推荐
     */
    private Map<String, Object> getFoodRecommendations(User user) {
        Map<String, Object> recommendations = new HashMap<>();

        Integer goalType = user.getGoalType();

        // 早餐推荐
        String[] breakfastOptions = {"燕麦片 + 牛奶 + 鸡蛋", "全麦面包 + 酸奶 + 水果", "玉米 + 豆浆 + 鸡蛋"};

        // 午餐推荐
        String[] lunchOptions;
        if (goalType != null && goalType == 1) { // 减脂
            lunchOptions = new String[]{"鸡胸肉沙拉 + 糙米饭", "清蒸鱼 + 西兰花 + 红薯", "虾仁炒蔬菜 + 杂粮饭"};
        } else if (goalType != null && goalType == 2) { // 增肌
            lunchOptions = new String[]{"牛肉 + 糙米饭 + 西兰花", "鸡胸肉 + 意面 + 蔬菜", "三文鱼 + 藜麦 + 芦笋"};
        } else { // 保持
            lunchOptions = new String[]{"瘦肉 + 米饭 + 时蔬", "鱼肉 + 杂粮饭 + 豆腐", "鸡腿 + 土豆 + 青菜"};
        }

        // 晚餐推荐
        String[] dinnerOptions;
        if (goalType != null && goalType == 1) {
            dinnerOptions = new String[]{"蔬菜汤 + 鸡胸肉", "豆腐 + 菌菇汤", "凉拌黄瓜 + 鱼肉"};
        } else {
            dinnerOptions = new String[]{"瘦肉粥 + 小菜", "蒸蛋 + 青菜", "清汤面 + 蔬菜"};
        }

        // 加餐推荐
        String[] snackOptions = {"苹果", "香蕉", "酸奶", "坚果一小把"};

        recommendations.put("breakfast", breakfastOptions);
        recommendations.put("lunch", lunchOptions);
        recommendations.put("dinner", dinnerOptions);
        recommendations.put("snack", snackOptions);

        return recommendations;
    }

    /**
     * 获取饮食提醒
     */
    private String getDietAdvice(User user, int todayIntake, int calorieGoal) {
        int remaining = calorieGoal - todayIntake;

        if (remaining <= 0) {
            return "⚠️ 今日热量已超标，建议增加运动消耗";
        } else if (remaining < 300) {
            return "🍎 今日还剩 " + remaining + " kcal，建议吃点水果或酸奶";
        } else if (remaining < 500) {
            return "🥗 今日还剩 " + remaining + " kcal，建议吃一份营养餐";
        } else {
            if (user.getGoalType() != null && user.getGoalType() == 1) {
                return "💪 今日还有 " + remaining + " kcal 预算，注意不要吃太少哦";
            } else {
                return "🍽️ 今日还有 " + remaining + " kcal 预算，可以享受美食";
            }
        }
    }
}
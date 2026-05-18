package com.eatnotfat.backend.service;

import com.eatnotfat.backend.entity.DietRecord;
import com.eatnotfat.backend.entity.DietRecordItem;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.entity.WeightRecord;
import com.eatnotfat.backend.mapper.DietRecordItemMapper;
import com.eatnotfat.backend.mapper.DietRecordMapper;
import com.eatnotfat.backend.mapper.UserMapper;
import com.eatnotfat.backend.mapper.WeightRecordMapper;
import com.eatnotfat.backend.vo.DashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DietRecordMapper dietRecordMapper;

    @Autowired
    private DietRecordItemMapper dietRecordItemMapper;

    @Autowired
    private WeightRecordMapper weightRecordMapper;

    /**
     * 获取完整的看板数据
     */
    public DashboardVO getDashboard(Long userId) {
        DashboardVO dashboard = new DashboardVO();
        dashboard.setTodaySummary(getTodaySummary(userId));
        dashboard.setNutritionStandard(getNutritionStandard(userId));
        dashboard.setWeeklyStats(getPeriodStats(userId, 7));
        dashboard.setMonthlyStats(getPeriodStats(userId, 30));
        dashboard.setAlerts(generateAlerts(userId));
        return dashboard;
    }

    // ==================== 今日摘要 ====================

    private DashboardVO.TodaySummary getTodaySummary(Long userId) {
        DashboardVO.TodaySummary summary = new DashboardVO.TodaySummary();

        User user = userMapper.selectById(userId);
        int target = getDailyCalorieGoal(user);
        summary.setTargetCalories(target);

        // 查询今日所有餐次记录
        String today = LocalDate.now().toString();
        List<DietRecord> records = dietRecordMapper.selectByDate(userId, today);

        int consumed = 0;
        double totalCarbs = 0, totalProtein = 0, totalFat = 0;
        List<DashboardVO.MealBrief> meals = new ArrayList<>();

        for (DietRecord record : records) {
            DashboardVO.MealBrief meal = new DashboardVO.MealBrief();
            meal.setType(record.getMealType());
            meal.setTypeName(getMealTypeName(record.getMealType()));
            int cal = record.getTotalCalorie() != null ? record.getTotalCalorie().intValue() : 0;
            meal.setCalories(cal);
            consumed += cal;

            // 查询该餐的明细，汇总营养素和食物名称
            List<DietRecordItem> items = dietRecordItemMapper.selectByRecordId(record.getId());
            StringBuilder foodsText = new StringBuilder();
            for (DietRecordItem item : items) {
                if (foodsText.length() > 0) foodsText.append("、");
                foodsText.append(item.getFoodName());

                if (item.getCarbs() != null) totalCarbs += item.getCarbs().doubleValue();
                if (item.getProtein() != null) totalProtein += item.getProtein().doubleValue();
                if (item.getFat() != null) totalFat += item.getFat().doubleValue();
            }
            meal.setFoods(foodsText.toString());
            meals.add(meal);
        }

        summary.setConsumedCalories(consumed);
        summary.setRemainingCalories(Math.max(0, target - consumed));
        summary.setProgressPercent(target > 0 ? Math.min(100.0, consumed * 100.0 / target) : 0);
        summary.setMeals(meals);

        // 宏量营养素
        DashboardVO.MacroDetail macros = new DashboardVO.MacroDetail();
        DashboardVO.NutritionStandard std = getNutritionStandard(userId);
        macros.setCarbs(new DashboardVO.MacroItem(
                round1(totalCarbs), std.getCarbsGram(), "g"));
        macros.setProtein(new DashboardVO.MacroItem(
                round1(totalProtein), std.getProteinGram(), "g"));
        macros.setFat(new DashboardVO.MacroItem(
                round1(totalFat), std.getFatGram(), "g"));
        summary.setMacros(macros);

        return summary;
    }

    // ==================== 个性化营养标准 ====================

    private DashboardVO.NutritionStandard getNutritionStandard(Long userId) {
        User user = userMapper.selectById(userId);
        DashboardVO.NutritionStandard std = new DashboardVO.NutritionStandard();

        if (user == null) {
            // 默认值
            std.setCalories(2000);
            std.setCarbsPercent(50);
            std.setProteinPercent(20);
            std.setFatPercent(30);
            std.setCarbsGram(250.0);
            std.setProteinGram(100.0);
            std.setFatGram(66.7);
            std.setDescription("默认营养标准");
            return std;
        }

        int goalType = user.getGoalType() != null ? user.getGoalType() : 1;
        int age = user.getAge() != null ? user.getAge() : 25;
        int gender = user.getGender() != null ? user.getGender() : 1;
        double weight = user.getCurrentWeight() != null ? user.getCurrentWeight().doubleValue() : 65;
        double height = user.getHeight() != null ? user.getHeight().doubleValue() : 170;
        int activityLevel = user.getActivityLevel() != null ? user.getActivityLevel() : 1;

        // BMR (Mifflin-St Jeor)
        double bmr = 10 * weight + 6.25 * height - 5 * age + (gender == 1 ? 5 : -161);
        double[] multipliers = {1.2, 1.375, 1.55, 1.725, 1.9};
        int idx = Math.max(0, Math.min(activityLevel - 1, 4));
        double tdee = bmr * multipliers[idx];

        int calories;
        int carbsP, proteinP, fatP;
        String desc;

        if (goalType == 1) { // 减脂
            calories = (int) Math.round(tdee * 0.8);
            carbsP = 40; proteinP = 30; fatP = 30;
            desc = "减脂模式：适度热量赤字，高蛋白保护肌肉";
        } else if (goalType == 2) { // 增肌
            calories = (int) Math.round(tdee * 1.1);
            carbsP = 45; proteinP = 30; fatP = 25;
            desc = "增肌模式：适度热量盈余，高蛋白促进肌肉合成";
        } else { // 保持
            calories = (int) Math.round(tdee);
            carbsP = 50; proteinP = 20; fatP = 30;
            desc = "保持模式：均衡营养，维持当前体重";
        }

        // 年龄微调
        if (age >= 40) {
            proteinP = Math.min(proteinP + 5, 35);
            carbsP = carbsP - 5;
            desc += "；40+岁建议提高蛋白质比例";
        }

        std.setCalories(calories);
        std.setCarbsPercent(carbsP);
        std.setProteinPercent(proteinP);
        std.setFatPercent(fatP);
        std.setCarbsGram(round1(calories * carbsP / 100.0 / 4.0));
        std.setProteinGram(round1(calories * proteinP / 100.0 / 4.0));
        std.setFatGram(round1(calories * fatP / 100.0 / 9.0));
        std.setDescription(desc);

        return std;
    }

    // ==================== 周期统计 ====================

    private DashboardVO.PeriodStats getPeriodStats(Long userId, int days) {
        DashboardVO.PeriodStats stats = new DashboardVO.PeriodStats();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        int target = getDailyCalorieGoal(userMapper.selectById(userId));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<DashboardVO.DailyCalorie> dailyList = new ArrayList<>();

        int totalCal = 0;
        int daysWithData = 0;
        int daysWithMultipleMeals = 0;
        Set<String> allFoodNames = new HashSet<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<DietRecord> records = dietRecordMapper.selectByDate(userId, date.toString());

            int dayCal = 0;
            Set<Integer> mealTypes = new HashSet<>();

            for (DietRecord record : records) {
                int cal = record.getTotalCalorie() != null ? record.getTotalCalorie().intValue() : 0;
                dayCal += cal;
                mealTypes.add(record.getMealType());

                List<DietRecordItem> items = dietRecordItemMapper.selectByRecordId(record.getId());
                for (DietRecordItem item : items) {
                    if (item.getFoodName() != null && !item.getFoodName().isEmpty()) {
                        allFoodNames.add(item.getFoodName());
                    }
                }
            }

            dailyList.add(new DashboardVO.DailyCalorie(date.format(fmt), dayCal, target));

            if (!records.isEmpty()) {
                totalCal += dayCal;
                daysWithData++;
                if (mealTypes.size() >= 2) {
                    daysWithMultipleMeals++;
                }
            }
        }

        stats.setDailyCalories(dailyList);
        stats.setAvgCalories(daysWithData > 0 ? totalCal / daysWithData : 0);

        // 用餐规律率
        stats.setMealRegularRate(daysWithData > 0
                ? round2(daysWithMultipleMeals * 1.0 / daysWithData) : 0);

        // 食物多样性评分
        int baseTarget = days <= 7 ? 15 : 40;
        stats.setFoodDiversityScore(Math.min(100,
                (int) Math.round(allFoodNames.size() * 100.0 / baseTarget)));

        // 热量波动（标准差）
        if (daysWithData > 1) {
            double avg = (double) totalCal / daysWithData;
            double sumSqDiff = 0;
            int count = 0;
            for (DashboardVO.DailyCalorie dc : dailyList) {
                if (dc.getCalories() > 0) {
                    sumSqDiff += Math.pow(dc.getCalories() - avg, 2);
                    count++;
                }
            }
            stats.setCalorieFluctuation(count > 1
                    ? (int) Math.round(Math.sqrt(sumSqDiff / (count - 1))) : 0);
        } else {
            stats.setCalorieFluctuation(0);
        }

        // 体重趋势
        List<WeightRecord> weightRecords = weightRecordMapper.selectRecent(userId, startDate);
        List<DashboardVO.WeightPoint> weightPoints = new ArrayList<>();
        for (WeightRecord wr : weightRecords) {
            if (wr.getWeight() != null && wr.getRecordDate() != null) {
                weightPoints.add(new DashboardVO.WeightPoint(
                        wr.getRecordDate().format(fmt),
                        wr.getWeight().doubleValue()));
            }
        }
        stats.setWeightTrend(weightPoints);

        return stats;
    }

    // ==================== 异常提醒 ====================

    private List<DashboardVO.Alert> generateAlerts(Long userId) {
        List<DashboardVO.Alert> alerts = new ArrayList<>();

        DashboardVO.TodaySummary today = getTodaySummary(userId);
        DashboardVO.NutritionStandard std = getNutritionStandard(userId);

        // 热量超标
        if (today.getConsumedCalories() > std.getCalories()) {
            int over = today.getConsumedCalories() - std.getCalories();
            alerts.add(new DashboardVO.Alert(
                    "calorie_over", "danger",
                    "热量超标提醒",
                    "今日已摄入 " + today.getConsumedCalories() + " kcal，超出目标 " + over + " kcal",
                    "建议下一餐以蔬菜、清汤为主，控制在200kcal以内"
            ));
        }

        // 营养素分析
        if (today.getMacros() != null) {
            DashboardVO.MacroItem carbs = today.getMacros().getCarbs();
            DashboardVO.MacroItem protein = today.getMacros().getProtein();
            DashboardVO.MacroItem fat = today.getMacros().getFat();

            if (protein.getTarget() != null && protein.getTarget() > 0
                    && protein.getActual() / protein.getTarget() < 0.5) {
                alerts.add(new DashboardVO.Alert(
                        "protein_low", "warning",
                        "蛋白质摄入不足",
                        "当前蛋白质 " + protein.getActual() + "g，仅达目标的 "
                                + Math.round(protein.getActual() / protein.getTarget() * 100) + "%",
                        "建议增加鸡蛋、鸡胸肉、鱼肉、豆腐等高蛋白食物"
                ));
            }

            if (fat.getTarget() != null && fat.getTarget() > 0
                    && fat.getActual() / fat.getTarget() > 1.3) {
                alerts.add(new DashboardVO.Alert(
                        "fat_high", "warning",
                        "脂肪摄入偏高",
                        "当前脂肪 " + fat.getActual() + "g，超出目标 "
                                + Math.round((fat.getActual() / fat.getTarget() - 1) * 100) + "%",
                        "建议减少油炸食品，选择蒸煮烹饪方式"
                ));
            }
        }

        // 今日未记录且已过下午2点
        if (today.getMeals().isEmpty() && java.time.LocalTime.now().getHour() >= 14) {
            alerts.add(new DashboardVO.Alert(
                    "no_record", "info",
                    "今日尚未记录饮食",
                    "已过午间，还没有记录任何餐次",
                    "及时记录饮食有助于追踪目标，点击下方快捷按钮记录"
            ));
        }

        return alerts;
    }

    // ==================== 工具方法 ====================

    private int getDailyCalorieGoal(User user) {
        if (user != null && user.getDailyCalorieGoal() != null) {
            return user.getDailyCalorieGoal();
        }
        return 2000;
    }

    private String getMealTypeName(int type) {
        switch (type) {
            case 1: return "早餐";
            case 2: return "午餐";
            case 3: return "晚餐";
            case 4: return "加餐";
            default: return "其他";
        }
    }

    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}

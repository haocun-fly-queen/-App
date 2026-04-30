package com.eatnotfat.backend.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.DietRecord;
import com.eatnotfat.backend.entity.DietRecordItem;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.DietRecordItemMapper;
import com.eatnotfat.backend.mapper.DietRecordMapper;
import com.eatnotfat.backend.service.DietRecordService;
import com.eatnotfat.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DietRecordServiceImpl extends ServiceImpl<DietRecordMapper, DietRecord> implements DietRecordService {

    @Autowired
    private DietRecordItemMapper dietRecordItemMapper;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public DietRecord saveDietRecord(Long userId, Integer mealType, List<DietRecordItem> items, String remark) {
        BigDecimal totalCalorie = items.stream()
                .map(DietRecordItem::getCalorie)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setMealType(mealType);
        record.setMealTime(LocalDateTime.now());
        record.setTotalCalorie(totalCalorie);
        record.setRemark(remark);

        this.save(record);

        for (int i = 0; i < items.size(); i++) {
            DietRecordItem item = items.get(i);
            item.setRecordId(record.getId());
            item.setSort(i);
            dietRecordItemMapper.insert(item);
        }

        return record;
    }

    @Override
    public List<DietRecord> getRecordsByDate(Long userId, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return this.baseMapper.selectByDate(userId, dateStr);
    }

    @Override
    public List<DietRecord> getRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return this.baseMapper.selectByTimeRange(userId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));
    }

    @Override
    public List<DietRecordItem> getRecordItems(Long recordId) {
        return dietRecordItemMapper.selectByRecordId(recordId);
    }

    @Override
    public Integer getDailyTotalCalories(Long userId, LocalDate date) {
        List<DietRecord> records = getRecordsByDate(userId, date);
        return records.stream()
                .map(DietRecord::getTotalCalorie)
                .filter(Objects::nonNull)
                .map(BigDecimal::intValue)
                .reduce(0, Integer::sum);
    }

    @Override
    @Transactional
    public void deleteDietRecord(Long recordId, Long userId) {
        DietRecord record = this.getById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在或无权限删除");
        }

        LambdaQueryWrapper<DietRecordItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecordItem::getRecordId, recordId);
        dietRecordItemMapper.delete(wrapper);

        this.removeById(recordId);
    }

    @Override
    public Map<String, Object> getWeeklyData(Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> weeklyData = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int totalCalories = getDailyTotalCalories(userId, date);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("dayOfWeek", getDayOfWeekChinese(date.getDayOfWeek().getValue()));
            dayData.put("totalCalories", totalCalories);
            weeklyData.add(dayData);
        }

        result.put("weeklyData", weeklyData);
        result.put("startDate", today.minusDays(6).toString());
        result.put("endDate", today.toString());

        return result;
    }

    @Override
    public Map<String, Object> getDashboardData(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // 打印日志
        System.out.println("========== 获取看板数据 ==========");
        System.out.println("用户ID: " + userId);
        System.out.println("开始日期: " + startDate);
        System.out.println("结束日期: " + endDate);

        // 1. 获取日期范围内的所有饮食记录
        List<DietRecord> records = this.baseMapper.selectByTimeRange(userId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));

        System.out.println("查询到记录数: " + records.size());

        // 2. 计算总热量和总餐数
        int totalCalories = 0;
        for (DietRecord record : records) {
            if (record.getTotalCalorie() != null) {
                totalCalories += record.getTotalCalorie().intValue();
            }
        }
        System.out.println("总热量: " + totalCalories);

        // 3. 获取每条记录的详情（用于营养素计算）
        List<DietRecordItem> allItems = new ArrayList<>();
        for (DietRecord record : records) {
            List<DietRecordItem> items = dietRecordItemMapper.selectByRecordId(record.getId());
            allItems.addAll(items);
        }

        // 4. 计算每日热量趋势
        Map<String, Integer> dailyCalories = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int total = 0;
            for (DietRecord record : records) {
                if (record.getMealTime().toLocalDate().equals(date) && record.getTotalCalorie() != null) {
                    total += record.getTotalCalorie().intValue();
                }
            }
            dailyCalories.put(date.toString(), total);
        }

        // 5. 计算营养素占比
        double totalCarbs = 0;
        double totalProtein = 0;
        double totalFat = 0;

        for (DietRecordItem item : allItems) {
            if (item.getCarbs() != null) {
                totalCarbs += item.getCarbs().doubleValue();
            }
            if (item.getProtein() != null) {
                totalProtein += item.getProtein().doubleValue();
            }
            if (item.getFat() != null) {
                totalFat += item.getFat().doubleValue();
            }
        }

        Map<String, Double> nutritionRatio = new HashMap<>();
        double total = totalCarbs + totalProtein + totalFat;
        if (total > 0) {
            nutritionRatio.put("carbs", (double) Math.round(totalCarbs / total * 100));
            nutritionRatio.put("protein", (double) Math.round(totalProtein / total * 100));
            nutritionRatio.put("fat", (double) Math.round(totalFat / total * 100));
        } else {
            nutritionRatio.put("carbs", 0.0);
            nutritionRatio.put("protein", 0.0);
            nutritionRatio.put("fat", 0.0);
        }

        // 6. 计算饮食规律分析
        int totalMeals = records.size();
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double avgMealsPerDay = days > 0 ? totalMeals * 1.0 / days : 0;
        int avgCaloriesPerDay = days > 0 ? (int) Math.round(totalCalories / (double) days) : 0;

        Map<String, Integer> mealTypeCount = new LinkedHashMap<>();
        mealTypeCount.put("早餐", 0);
        mealTypeCount.put("午餐", 0);
        mealTypeCount.put("晚餐", 0);
        mealTypeCount.put("加餐", 0);

        for (DietRecord record : records) {
            String mealName = getMealTypeName(record.getMealType());
            mealTypeCount.put(mealName, mealTypeCount.getOrDefault(mealName, 0) + 1);
        }

        // 7. 获取用户热量目标
        User user = userService.getById(userId);
        int calorieGoal = (user != null && user.getDailyCalorieGoal() != null) ? user.getDailyCalorieGoal() : 2000;

        // 8. 构建返回结果
        result.put("dailyCalories", dailyCalories);
        result.put("nutritionRatio", nutritionRatio);
        result.put("avgMealsPerDay", String.format("%.1f", avgMealsPerDay));
        result.put("totalMeals", totalMeals);
        result.put("mealTypeCount", mealTypeCount);
        result.put("totalCalories", totalCalories);
        result.put("avgCaloriesPerDay", avgCaloriesPerDay);
        result.put("calorieGoal", calorieGoal);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());

        System.out.println("返回数据: totalCalories=" + totalCalories + ", totalMeals=" + totalMeals);

        return result;
    }

    private String getMealTypeName(Integer mealType) {
        switch (mealType) {
            case 1: return "早餐";
            case 2: return "午餐";
            case 3: return "晚餐";
            case 4: return "加餐";
            default: return "未知";
        }
    }

    private String getDayOfWeekChinese(int dayOfWeek) {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return days[dayOfWeek - 1];
    }


    @Override
    public Map<String, Double> getTodayNutrients(Long userId, LocalDate date) {
        Map<String, Double> nutrients = new HashMap<>();

        // 获取当天的饮食记录
        List<DietRecord> records = getRecordsByDate(userId, date);

        double totalCarbs = 0;
        double totalProtein = 0;
        double totalFat = 0;

        // 遍历每条记录，获取详情并累加营养素
        for (DietRecord record : records) {
            List<DietRecordItem> items = dietRecordItemMapper.selectByRecordId(record.getId());
            for (DietRecordItem item : items) {
                if (item.getCarbs() != null) {
                    totalCarbs += item.getCarbs().doubleValue();
                }
                if (item.getProtein() != null) {
                    totalProtein += item.getProtein().doubleValue();
                }
                if (item.getFat() != null) {
                    totalFat += item.getFat().doubleValue();
                }
            }
        }

        nutrients.put("carbs", Math.round(totalCarbs * 10) / 10.0);
        nutrients.put("protein", Math.round(totalProtein * 10) / 10.0);
        nutrients.put("fat", Math.round(totalFat * 10) / 10.0);

        return nutrients;
    }
    @Override
    public Map<String, String> getFirstRecordDate(Long userId) {
        Map<String, String> result = new HashMap<>();

        // 查询用户最早的饮食记录日期
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecord::getUserId, userId)
                .orderByAsc(DietRecord::getMealTime)
                .last("limit 1");
        DietRecord firstRecord = this.getOne(wrapper);

        // 查询用户最新的饮食记录日期
        LambdaQueryWrapper<DietRecord> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(DietRecord::getUserId, userId)
                .orderByDesc(DietRecord::getMealTime)
                .last("limit 1");
        DietRecord lastRecord = this.getOne(wrapper2);

        result.put("firstDate", firstRecord != null ?
                firstRecord.getMealTime().toLocalDate().toString() :
                LocalDate.now().toString());
        result.put("lastDate", lastRecord != null ?
                lastRecord.getMealTime().toLocalDate().toString() :
                LocalDate.now().toString());

        return result;
    }

}
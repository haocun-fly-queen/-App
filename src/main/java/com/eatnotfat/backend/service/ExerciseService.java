package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eatnotfat.backend.dto.ExercisePlanRequest;
import com.eatnotfat.backend.dto.ExerciseRecordDTO;
import com.eatnotfat.backend.dto.ExerciseReminderDTO;
import com.eatnotfat.backend.entity.*;
import com.eatnotfat.backend.mapper.*;
import com.eatnotfat.backend.vo.ExerciseCalendarVO;
import com.eatnotfat.backend.vo.ExerciseDashboardVO;
import com.eatnotfat.backend.vo.ExerciseStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExerciseService {

    @Autowired
    private ExerciseLibraryMapper exerciseLibraryMapper;

    @Autowired
    private ExerciseRecordMapper exerciseRecordMapper;

    @Autowired
    private ExercisePlanMapper exercisePlanMapper;

    @Autowired
    private ExerciseReminderMapper exerciseReminderMapper;

    @Autowired
    private DietRecordMapper dietRecordMapper;

    @Autowired
    private DietRecordItemMapper dietRecordItemMapper;

    @Autowired
    private UserMapper userMapper;

    // ==================== 运动库 ====================

    public List<ExerciseLibrary> getAllExercises() {
        return exerciseLibraryMapper.selectAllEnabled();
    }

    public List<ExerciseLibrary> getExercisesByCategory(String category) {
        return exerciseLibraryMapper.selectByCategory(category);
    }

    // ==================== 运动记录 CRUD ====================

    public ExerciseRecord saveRecord(ExerciseRecordDTO dto) {
        User user = userMapper.selectById(dto.getUserId());
        double weightKg = user != null && user.getCurrentWeight() != null
                ? user.getCurrentWeight().doubleValue() : 65.0;

        ExerciseRecord record = new ExerciseRecord();
        record.setUserId(dto.getUserId());
        record.setExerciseId(dto.getExerciseId());
        record.setExerciseName(dto.getExerciseName());
        record.setCategory(dto.getCategory());
        record.setDurationMinutes(dto.getDurationMinutes());
        record.setIntensity(dto.getIntensity() != null ? dto.getIntensity() : 2);
        record.setFeeling(dto.getFeeling());
        record.setRemark(dto.getRemark());
        record.setRecordTime(dto.getRecordTime() != null ? dto.getRecordTime() : LocalDateTime.now());

        if (dto.getCaloriesBurned() != null && dto.getCaloriesBurned().doubleValue() > 0) {
            record.setCaloriesBurned(dto.getCaloriesBurned());
        } else {
            double calories = calculateCalories(dto.getExerciseId(), weightKg, dto.getDurationMinutes());
            record.setCaloriesBurned(BigDecimal.valueOf(calories).setScale(1, RoundingMode.HALF_UP));
        }

        if (dto.getCategory() == null && dto.getExerciseId() != null) {
            ExerciseLibrary lib = exerciseLibraryMapper.selectById(dto.getExerciseId());
            if (lib != null) {
                record.setCategory(lib.getCategory());
                if (dto.getExerciseName() == null) {
                    record.setExerciseName(lib.getName());
                }
            }
        }

        exerciseRecordMapper.insert(record);
        return record;
    }

    public void deleteRecord(Long recordId, Long userId) {
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("id", recordId).eq("user_id", userId);
        exerciseRecordMapper.delete(wrapper);
    }

    public List<ExerciseRecord> getRecords(Long userId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        return exerciseRecordMapper.selectByDateRange(userId, startTime);
    }

    public List<ExerciseRecord> getTodayRecords(Long userId) {
        String today = LocalDate.now().toString();
        return exerciseRecordMapper.selectByDate(userId, today);
    }

    // ==================== 热量计算 ====================

    public double calculateCalories(Long exerciseId, double weightKg, int durationMinutes) {
        double metValue = 5.0;
        if (exerciseId != null) {
            ExerciseLibrary lib = exerciseLibraryMapper.selectById(exerciseId);
            if (lib != null && lib.getMetValue() != null) {
                metValue = lib.getMetValue().doubleValue();
            }
        }
        double hours = durationMinutes / 60.0;
        return metValue * weightKg * hours;
    }

    // ==================== 看板数据 ====================

    public ExerciseDashboardVO getDashboard(Long userId) {
        ExerciseDashboardVO dashboard = new ExerciseDashboardVO();

        List<ExerciseRecord> todayRecords = getTodayRecords(userId);
        ExerciseDashboardVO.TodayExercise today = buildTodayExercise(userId, todayRecords);
        dashboard.setTodayExercise(today);

        ExercisePlan plan = exercisePlanMapper.selectByDate(userId, LocalDate.now());
        if (plan != null && plan.getPlanContent() != null) {
            dashboard.setTodayPlan(parsePlanContent(plan.getPlanContent()));
        } else {
            ExerciseDashboardVO.ExercisePlanVO emptyPlan = new ExerciseDashboardVO.ExercisePlanVO();
            emptyPlan.setGenerated(false);
            emptyPlan.setExercises(new ArrayList<>());
            dashboard.setTodayPlan(emptyPlan);
        }

        dashboard.setFatigue(getFatigueStatus(userId));
        return dashboard;
    }

    private ExerciseDashboardVO.TodayExercise buildTodayExercise(Long userId, List<ExerciseRecord> records) {
        ExerciseDashboardVO.TodayExercise today = new ExerciseDashboardVO.TodayExercise();

        User user = userMapper.selectById(userId);
        int targetCalories = 300;
        if (user != null) {
            double bmr = calculateBMR(user);
            double tdee = bmr * getActivityMultiplier(user.getActivityLevel());
            targetCalories = (int) Math.round(tdee * 0.12);
        }
        today.setTargetCalories(targetCalories);

        int consumed = 0;
        List<ExerciseDashboardVO.ExerciseBrief> briefs = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (ExerciseRecord r : records) {
            double cal = r.getCaloriesBurned() != null ? r.getCaloriesBurned().doubleValue() : 0;
            consumed += cal;

            ExerciseDashboardVO.ExerciseBrief brief = new ExerciseDashboardVO.ExerciseBrief();
            brief.setId(r.getId());
            brief.setExerciseName(r.getExerciseName());
            brief.setDurationMinutes(r.getDurationMinutes());
            brief.setCaloriesBurned(cal);
            brief.setIntensity(r.getIntensity());
            brief.setRecordTime(r.getRecordTime() != null ? r.getRecordTime().format(timeFmt) : "");

            if (r.getExerciseId() != null) {
                ExerciseLibrary lib = exerciseLibraryMapper.selectById(r.getExerciseId());
                if (lib != null) brief.setIcon(lib.getIcon());
            }
            if (brief.getIcon() == null) brief.setIcon("🏃");

            briefs.add(brief);
        }

        today.setConsumedCalories(consumed);
        today.setRemainingCalories(Math.max(0, targetCalories - consumed));
        today.setProgressPercent(targetCalories > 0 ? Math.min(100.0, consumed * 100.0 / targetCalories) : 0);
        today.setRecords(briefs);

        return today;
    }

    // ==================== 日历数据 ====================

    public ExerciseCalendarVO getCalendar(Long userId, int year, int month) {
        ExerciseCalendarVO calendar = new ExerciseCalendarVO();
        calendar.setYear(year);
        calendar.setMonth(month);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<ExerciseCalendarVO.DayRecord> days = new ArrayList<>();
        int totalDays = 0;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            List<ExerciseRecord> records = exerciseRecordMapper.selectByDate(userId, date.toString());

            ExerciseCalendarVO.DayRecord day = new ExerciseCalendarVO.DayRecord();
            day.setDate(date.toString());
            day.setHasExercise(!records.isEmpty());
            day.setRecordCount(records.size());

            double totalCal = 0;
            for (ExerciseRecord r : records) {
                totalCal += (r.getCaloriesBurned() != null ? r.getCaloriesBurned().doubleValue() : 0);
            }
            day.setTotalCalories(totalCal);
            days.add(day);

            if (!records.isEmpty()) totalDays++;
        }

        calendar.setDays(days);
        calendar.setTotalDays(totalDays);
        return calendar;
    }

    // ==================== 统计数据 ====================

    public ExerciseStatsVO getStats(Long userId, int days) {
        ExerciseStatsVO stats = new ExerciseStatsVO();
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        List<ExerciseRecord> allRecords = exerciseRecordMapper.selectByDateRange(userId, startTime);

        Map<String, List<ExerciseRecord>> byDate = allRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getRecordTime().toLocalDate().toString()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        LocalDate endDate = LocalDate.now();
        User user = userMapper.selectById(userId);
        int dailyTarget = 300;
        if (user != null) {
            double bmr = calculateBMR(user);
            double tdee = bmr * getActivityMultiplier(user.getActivityLevel());
            dailyTarget = (int) Math.round(tdee * 0.12);
        }

        List<ExerciseStatsVO.DailyExercise> dailyList = new ArrayList<>();
        int totalCal = 0;
        int totalDuration = 0;
        Set<String> typeNames = new HashSet<>();
        Map<String, Integer> categoryCount = new HashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<ExerciseRecord> dayRecords = byDate.getOrDefault(date.toString(), Collections.emptyList());
            int dayCal = 0;
            int dayDuration = 0;
            for (ExerciseRecord r : dayRecords) {
                dayCal += (r.getCaloriesBurned() != null ? r.getCaloriesBurned().intValue() : 0);
                dayDuration += r.getDurationMinutes();
                typeNames.add(r.getExerciseName());
                String cat = r.getCategory() != null ? r.getCategory() : "other";
                categoryCount.merge(cat, 1, Integer::sum);
            }
            ExerciseStatsVO.DailyExercise de = new ExerciseStatsVO.DailyExercise();
            de.setDate(date.format(fmt));
            de.setCalories(dayCal);
            de.setTarget(dailyTarget);
            de.setDuration(dayDuration);
            dailyList.add(de);

            totalCal += dayCal;
            totalDuration += dayDuration;
        }
        stats.setDailyExercises(dailyList);

        int activeDays = byDate.size();
        stats.setTotalDays(activeDays);
        stats.setTotalCalories(totalCal);
        stats.setAvgDuration(activeDays > 0 ? totalDuration / activeDays : 0);
        stats.setExerciseTypeCount(typeNames.size());

        int totalRecords = allRecords.size();
        List<ExerciseStatsVO.CategoryRatio> ratios = new ArrayList<>();
        String[][] categoryNames = {
                {"cardio", "有氧"}, {"strength", "无氧"}, {"flexibility", "拉伸"}, {"hiit", "HIIT"}
        };
        for (String[] cn : categoryNames) {
            int count = categoryCount.getOrDefault(cn[0], 0);
            if (count > 0) {
                ExerciseStatsVO.CategoryRatio ratio = new ExerciseStatsVO.CategoryRatio();
                ratio.setCategory(cn[0]);
                ratio.setCategoryName(cn[1]);
                ratio.setCount(count);
                ratio.setPercent(totalRecords > 0 ? Math.round(count * 100.0 / totalRecords) : 0);
                ratios.add(ratio);
            }
        }
        stats.setCategoryRatios(ratios);

        List<ExerciseStatsVO.IntakeVsBurn> intakeVsBurn = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<DietRecord> dietRecords = dietRecordMapper.selectByDate(userId, date.toString());
            int intake = 0;
            for (DietRecord dr : dietRecords) {
                intake += (dr.getTotalCalorie() != null ? dr.getTotalCalorie().intValue() : 0);
            }

            List<ExerciseRecord> exRecords = byDate.getOrDefault(date.toString(), Collections.emptyList());
            int burn = 0;
            for (ExerciseRecord er : exRecords) {
                burn += (er.getCaloriesBurned() != null ? er.getCaloriesBurned().intValue() : 0);
            }

            ExerciseStatsVO.IntakeVsBurn ivb = new ExerciseStatsVO.IntakeVsBurn();
            ivb.setDate(date.format(fmt));
            ivb.setIntake(intake);
            ivb.setBurn(burn);
            ivb.setNet(intake - burn);
            intakeVsBurn.add(ivb);
        }
        stats.setIntakeVsBurn(intakeVsBurn);

        return stats;
    }

    // ==================== 疲劳度分析 ====================

    public ExerciseDashboardVO.FatigueStatus getFatigueStatus(Long userId) {
        ExerciseDashboardVO.FatigueStatus status = new ExerciseDashboardVO.FatigueStatus();

        int consecutive = calculateConsecutiveDays(userId);

        status.setConsecutiveDays(consecutive);
        status.setNeedRest(consecutive >= 4);

        if (consecutive == 0) {
            status.setStatusText("今天还没有运动");
            status.setSuggestion("开始你的第一次运动吧！");
        } else if (consecutive <= 2) {
            status.setStatusText("运动状态良好");
            status.setSuggestion("保持节奏，注意运动后拉伸放松");
        } else if (consecutive == 3) {
            status.setStatusText("已连续运动3天");
            status.setSuggestion("明天建议适当降低运动强度，或安排轻度拉伸");
        } else if (consecutive == 4) {
            status.setStatusText("已连续运动4天");
            status.setSuggestion("肌肉需要48-72小时恢复，建议今天休息或只做轻度拉伸");
        } else {
            status.setStatusText("已连续运动" + consecutive + "天");
            status.setSuggestion("运动过度可能导致受伤，强烈建议今天安排休息日");
        }

        return status;
    }

    // ==================== 提醒设置 ====================

    public ExerciseReminder getReminder(Long userId) {
        return exerciseReminderMapper.selectByUserId(userId);
    }

    public ExerciseReminder saveReminder(ExerciseReminderDTO dto) {
        ExerciseReminder existing = exerciseReminderMapper.selectByUserId(dto.getUserId());

        if (existing != null) {
            existing.setRemindTime(LocalTime.parse(dto.getRemindTime()));
            existing.setRemindDays(dto.getRemindDays());
            existing.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : 1);
            exerciseReminderMapper.updateById(existing);
            return existing;
        } else {
            ExerciseReminder reminder = new ExerciseReminder();
            reminder.setUserId(dto.getUserId());
            reminder.setRemindTime(LocalTime.parse(dto.getRemindTime()));
            reminder.setRemindDays(dto.getRemindDays() != null ? dto.getRemindDays() : "1,2,3,4,5");
            reminder.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : 1);
            exerciseReminderMapper.insert(reminder);
            return reminder;
        }
    }

    // ==================== AI规划数据组装 ====================

    public ExercisePlanRequest buildPlanRequest(Long userId) {
        ExercisePlanRequest req = new ExercisePlanRequest();
        req.setUserId(userId);

        User user = userMapper.selectById(userId);
        if (user == null) return req;

        // ========== 用户档案 ==========
        ExercisePlanRequest.UserProfile profile = new ExercisePlanRequest.UserProfile();

        profile.setGender(user.getGender());
        profile.setGenderLabel(user.getGender() == 1 ? "男" : (user.getGender() == 2 ? "女" : "未设置"));
        profile.setAge(user.getAge() != null ? user.getAge() : 25);
        profile.setHeight(user.getHeight() != null ? user.getHeight().doubleValue() : 170.0);
        profile.setWeight(user.getCurrentWeight() != null ? user.getCurrentWeight().doubleValue() : 65.0);
        profile.setTargetWeight(user.getTargetWeight() != null ? user.getTargetWeight().doubleValue() : profile.getWeight());

        double weightDiff = profile.getWeight() - profile.getTargetWeight();
        profile.setWeightDiff(Math.round(weightDiff * 10.0) / 10.0);

        int goal = user.getGoalType() != null ? user.getGoalType() : 1;
        profile.setGoalType(goal);
        profile.setGoalLabel(goal == 1 ? "减脂" : (goal == 2 ? "增肌" : "维持"));

        int act = user.getActivityLevel() != null ? user.getActivityLevel() : 1;
        profile.setActivityLevel(act);
        String[] actLabels = {"", "久坐", "轻度活动", "中度活动", "重度活动", "极重度活动"};
        profile.setActivityLabel(act >= 1 && act <= 5 ? actLabels[act] : "久坐");

        int exLevel = user.getExerciseLevel() != null ? user.getExerciseLevel() : 1;
        profile.setExerciseLevel(exLevel);
        String[] exLabels = {"", "新手", "进阶", "高级"};
        profile.setExerciseLevelLabel(exLevel >= 1 && exLevel <= 3 ? exLabels[exLevel] : "新手");

        // BMR（Mifflin-St Jeor）
        double bmr;
        if (user.getGender() == 2) {
            bmr = 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * profile.getAge() - 161;
        } else {
            bmr = 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * profile.getAge() + 5;
        }
        profile.setBmr((double) Math.round(bmr));

        // TDEE
        double[] factors = {0, 1.2, 1.375, 1.55, 1.725, 1.9};
        double tdee = bmr * factors[act >= 1 && act <= 5 ? act : 1];
        profile.setTdee((double) Math.round(tdee));

        // 目标热量
        double targetCal;
        if (goal == 1) targetCal = tdee * 0.8;
        else if (goal == 2) targetCal = tdee * 1.1;
        else targetCal = tdee;
        profile.setTargetCalorie((double) Math.round(targetCal));

        // 运动消耗建议
        profile.setExerciseCalorieGoal((double) Math.round(tdee * 0.12));

        req.setProfile(profile);

        // ========== 近7日饮食数据（直接计算）==========
        ExercisePlanRequest.WeeklyDietData dietData = new ExercisePlanRequest.WeeklyDietData();
        int target = profile.getTargetCalorie() != null ? profile.getTargetCalorie().intValue() : 2000;
        dietData.setTargetCalories(target);

        int totalCal = 0;
        int surplusDays = 0;
        int totalSurplus = 0;
        int daysWithData = 0;

        LocalDate dietStart = LocalDate.now().minusDays(6);
        LocalDate dietEnd = LocalDate.now();

        for (LocalDate date = dietStart; !date.isAfter(dietEnd); date = date.plusDays(1)) {
            List<DietRecord> dietRecords = dietRecordMapper.selectByDate(userId, date.toString());
            int dayCal = 0;
            for (DietRecord dr : dietRecords) {
                dayCal += (dr.getTotalCalorie() != null ? dr.getTotalCalorie().intValue() : 0);
            }
            if (!dietRecords.isEmpty()) {
                totalCal += dayCal;
                daysWithData++;
                if (dayCal > target) {
                    surplusDays++;
                    totalSurplus += (dayCal - target);
                }
            }
        }

        dietData.setAvgCalories(daysWithData > 0 ? totalCal / daysWithData : 0);
        dietData.setSurplusDays(surplusDays);
        dietData.setTotalSurplus(totalSurplus);
        req.setWeeklyDiet(dietData);

        // ========== 近7日运动数据 ==========
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<ExerciseRecord> recentRecords = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .ge(ExerciseRecord::getRecordTime, sevenDaysAgo)
                        .orderByDesc(ExerciseRecord::getRecordTime)
        );

        ExercisePlanRequest.WeeklyExerciseData exData = new ExercisePlanRequest.WeeklyExerciseData();
        if (!recentRecords.isEmpty()) {
            exData.setActiveDays((int) recentRecords.stream()
                    .map(r -> r.getRecordTime().toLocalDate())
                    .distinct().count());
            exData.setTotalBurned(recentRecords.stream()
                    .map(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned().intValue() : 0)
                    .reduce(0, Integer::sum));
            exData.setAvgDuration(exData.getActiveDays() > 0 ?
                    (int) recentRecords.stream().mapToInt(ExerciseRecord::getDurationMinutes).average().orElse(0) : 0);
            exData.setRecentTypes(recentRecords.stream()
                    .map(ExerciseRecord::getExerciseName).distinct().limit(5).collect(Collectors.toList()));
        }
        req.setWeeklyExercise(exData);

        // ========== 疲劳度 ==========
        ExercisePlanRequest.FatigueData fatigueData = new ExercisePlanRequest.FatigueData();
        int consecutive = calculateConsecutiveDays(userId);
        fatigueData.setConsecutiveDays(consecutive);
        fatigueData.setNeedRest(consecutive >= 4);
        req.setFatigue(fatigueData);

        return req;
    }

    // ==================== 解析AI计划JSON ====================

    private ExerciseDashboardVO.ExercisePlanVO parsePlanContent(String json) {
        ExerciseDashboardVO.ExercisePlanVO plan = new ExerciseDashboardVO.ExercisePlanVO();
        try {
            com.alibaba.fastjson2.JSONObject root = com.alibaba.fastjson2.JSONObject.parseObject(json);

            com.alibaba.fastjson2.JSONArray exercisesArr = root.getJSONArray("exercises");
            List<ExerciseDashboardVO.PlanExercise> exercises = new ArrayList<>();
            if (exercisesArr != null) {
                for (int i = 0; i < exercisesArr.size(); i++) {
                    com.alibaba.fastjson2.JSONObject ex = exercisesArr.getJSONObject(i);
                    ExerciseDashboardVO.PlanExercise pe = new ExerciseDashboardVO.PlanExercise();
                    pe.setName(ex.getString("name"));
                    pe.setIcon(ex.getString("icon"));
                    pe.setCategory(ex.getString("category"));
                    pe.setDuration(ex.getIntValue("duration"));
                    pe.setCalories(ex.getIntValue("calories"));
                    pe.setIntensity(ex.getString("intensity"));
                    pe.setReason(ex.getString("reason"));
                    exercises.add(pe);
                }
            }
            plan.setExercises(exercises);
            plan.setTotalDuration(root.getIntValue("totalDuration"));
            plan.setTotalCalories(root.getIntValue("totalCalories"));
            plan.setAdvice(root.getString("advice"));
            plan.setGenerated(true);
        } catch (Exception e) {
            plan.setGenerated(false);
            plan.setExercises(new ArrayList<>());
        }
        return plan;
    }

    // ==================== 工具方法 ====================

    public ExercisePlan getPlanByDate(Long userId, LocalDate date) {
        return exercisePlanMapper.selectByDate(userId, date);
    }

    public void updatePlan(ExercisePlan plan) {
        exercisePlanMapper.updateById(plan);
    }

    public void insertPlan(ExercisePlan plan) {
        exercisePlanMapper.insert(plan);
    }

    public ExerciseDashboardVO.ExercisePlanVO parsePlanFromJson(String json) {
        return parsePlanContent(json);
    }

    private double calculateBMR(User user) {
        double weight = user.getCurrentWeight() != null ? user.getCurrentWeight().doubleValue() : 65;
        double height = user.getHeight() != null ? user.getHeight().doubleValue() : 170;
        int age = user.getAge() != null ? user.getAge() : 25;
        int gender = user.getGender() != null ? user.getGender() : 1;
        return 10 * weight + 6.25 * height - 5 * age + (gender == 1 ? 5 : -161);
    }

    private double getActivityMultiplier(int activityLevel) {
        double[] multipliers = {1.2, 1.375, 1.55, 1.725, 1.9};
        int idx = Math.max(0, Math.min(activityLevel - 1, 4));
        return multipliers[idx];
    }

    /**
     * 计算连续运动天数（从今天往前推）
     */
    private int calculateConsecutiveDays(Long userId) {
        int consecutive = 0;
        LocalDate checkDate = LocalDate.now();
        for (int i = 0; i < 30; i++) {
            List<ExerciseRecord> records = exerciseRecordMapper.selectByDate(userId, checkDate.toString());
            if (!records.isEmpty()) {
                consecutive++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }
        return consecutive;
    }
}

package com.eatnotfat.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatnotfat.backend.entity.AiLog;
import com.eatnotfat.backend.entity.DietRecord;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.AiLogMapper;
import com.eatnotfat.backend.mapper.DietRecordMapper;
import com.eatnotfat.backend.mapper.UserMapper;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DietRecordMapper dietRecordMapper;

    @Autowired
    private AiLogMapper aiLogMapper;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        Map<String, Object> result = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        LocalDate weekStart = today.minusDays(6);
        LocalDateTime weekStartTime = weekStart.atStartOfDay();

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartTime = monthStart.atStartOfDay();

        // ========== 用户相关统计 ==========
        Long totalUsers = userMapper.selectCount(null);

        LambdaQueryWrapper<User> todayUserWrapper = new LambdaQueryWrapper<>();
        todayUserWrapper.ge(User::getCreateTime, todayStart)
                .le(User::getCreateTime, todayEnd);
        Long todayNewUsers = userMapper.selectCount(todayUserWrapper);

        LambdaQueryWrapper<User> weekUserWrapper = new LambdaQueryWrapper<>();
        weekUserWrapper.ge(User::getCreateTime, weekStartTime);
        Long weekNewUsers = userMapper.selectCount(weekUserWrapper);

        LambdaQueryWrapper<User> monthUserWrapper = new LambdaQueryWrapper<>();
        monthUserWrapper.ge(User::getCreateTime, monthStartTime);
        Long monthNewUsers = userMapper.selectCount(monthUserWrapper);

        // ========== 日活统计（今日有饮食记录的用户数）- 使用 meal_time ==========
        LambdaQueryWrapper<DietRecord> todayDietWrapper = new LambdaQueryWrapper<>();
        todayDietWrapper.ge(DietRecord::getMealTime, todayStart)
                .le(DietRecord::getMealTime, todayEnd)
                .groupBy(DietRecord::getUserId);
        List<DietRecord> todayDietRecords = dietRecordMapper.selectList(todayDietWrapper);
        Long dailyActiveUsers = (long) todayDietRecords.stream()
                .map(DietRecord::getUserId)
                .distinct()
                .count();

        // ========== 今日饮食记录量 - 使用 meal_time ==========
        LambdaQueryWrapper<DietRecord> dietCountWrapper = new LambdaQueryWrapper<>();
        dietCountWrapper.ge(DietRecord::getMealTime, todayStart)
                .le(DietRecord::getMealTime, todayEnd);
        Long todayDietCount = dietRecordMapper.selectCount(dietCountWrapper);

        // ========== AI识别调用量 ==========
        LambdaQueryWrapper<AiLog> aiLogWrapper = new LambdaQueryWrapper<>();
        aiLogWrapper.ge(AiLog::getCreateTime, todayStart)
                .le(AiLog::getCreateTime, todayEnd);
        Long todayAiCalls = aiLogMapper.selectCount(aiLogWrapper);

        // ========== 用户目标分布 ==========
        LambdaQueryWrapper<User> goalWrapper = new LambdaQueryWrapper<>();
        goalWrapper.isNotNull(User::getGoalType);
        List<User> usersWithGoal = userMapper.selectList(goalWrapper);

        long fatLossCount = usersWithGoal.stream().filter(u -> u.getGoalType() == 1).count();
        long muscleGainCount = usersWithGoal.stream().filter(u -> u.getGoalType() == 2).count();
        long maintainCount = usersWithGoal.stream().filter(u -> u.getGoalType() == 3).count();

        Map<String, Long> goalDistribution = new HashMap<>();
        goalDistribution.put("减脂", fatLossCount);
        goalDistribution.put("增肌", muscleGainCount);
        goalDistribution.put("保持体重", maintainCount);

        // ========== 近7天新增趋势 ==========
        Map<String, Long> last7DaysNewUsers = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString().substring(5);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getCreateTime, dayStart)
                    .le(User::getCreateTime, dayEnd);
            Long count = userMapper.selectCount(wrapper);
            last7DaysNewUsers.put(dateStr, count);
        }

        // ========== 近7天AI调用趋势 ==========
        Map<String, Long> last7DaysAiCalls = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString().substring(5);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            LambdaQueryWrapper<AiLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(AiLog::getCreateTime, dayStart)
                    .le(AiLog::getCreateTime, dayEnd);
            Long count = aiLogMapper.selectCount(wrapper);
            last7DaysAiCalls.put(dateStr, count);
        }

        // ========== 组装返回数据 ==========
        result.put("totalUsers", totalUsers);
        result.put("todayNewUsers", todayNewUsers);
        result.put("weekNewUsers", weekNewUsers);
        result.put("monthNewUsers", monthNewUsers);
        result.put("dailyActiveUsers", dailyActiveUsers);
        result.put("todayDietCount", todayDietCount);
        result.put("todayAiCalls", todayAiCalls);
        result.put("goalDistribution", goalDistribution);
        result.put("last7DaysNewUsers", last7DaysNewUsers);
        result.put("last7DaysAiCalls", last7DaysAiCalls);

        return Result.success(result);
    }
}
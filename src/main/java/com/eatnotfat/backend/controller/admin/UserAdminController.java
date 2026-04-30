package com.eatnotfat.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatnotfat.backend.entity.AiLog;
import com.eatnotfat.backend.entity.DietRecord;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.AiLogMapper;
import com.eatnotfat.backend.mapper.DietRecordMapper;
import com.eatnotfat.backend.service.DietRecordService;
import com.eatnotfat.backend.service.UserService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/user")
public class UserAdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private DietRecordService dietRecordService;

    @Autowired
    private DietRecordMapper dietRecordMapper;

    @Autowired
    private AiLogMapper aiLogMapper;  // 新增

    // ... 其他已有方法保持不变 ...

    /**
     * 获取用户列表（分页 + 搜索），并附带近7天活跃天数
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        // 1. 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);

        // 2. 执行分页查询
        Page<User> pageResult = userService.page(new Page<>(page, size), wrapper);
        List<User> userList = pageResult.getRecords();

        if (userList.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", Collections.emptyList());
            result.put("total", 0L);
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        }

        // 3. 批量查询所有用户的近7天活跃天数
        List<Long> userIds = userList.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, Integer> activeDaysMap = getActiveDaysBatch(userIds);

        // 4. 构建返回结果
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (User user : userList) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("nickname", user.getNickname());
            userMap.put("phone", user.getPhone());
            userMap.put("gender", user.getGender());
            userMap.put("height", user.getHeight());
            userMap.put("currentWeight", user.getCurrentWeight());
            userMap.put("goalType", user.getGoalType());
            userMap.put("createTime", user.getCreateTime());
            userMap.put("status", user.getStatus());
            userMap.put("avatarUrl", user.getAvatarUrl());
            userMap.put("age", user.getAge());
            userMap.put("activityLevel", user.getActivityLevel());
            userMap.put("targetWeight", user.getTargetWeight());
            userMap.put("dailyCalorieGoal", user.getDailyCalorieGoal());
            // 新增：近7天活跃天数
            userMap.put("activeDays7", activeDaysMap.getOrDefault(user.getId(), 0));
            resultList.add(userMap);
        }

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", resultList);
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 批量获取用户的近7天活跃天数
     */
    private Map<Long, Integer> getActiveDaysBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 计算7天前的起始时间（从今天往前推6天，共7天）
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = LocalDateTime.now();

        // ✅ 修复：使用 COUNT(DISTINCT DATE(meal_time)) 一次性统计
        QueryWrapper<DietRecord> wrapper = new QueryWrapper<>();
        wrapper.select("user_id, COUNT(DISTINCT DATE(meal_time)) as active_days")
                .in("user_id", userIds)
                .ge("meal_time", startDateTime)
                .le("meal_time", endDateTime)
                .groupBy("user_id");

        List<Map<String, Object>> records = dietRecordMapper.selectMaps(wrapper);

        // 转换结果
        Map<Long, Integer> activeDaysMap = new HashMap<>();
        for (Map<String, Object> record : records) {
            Long userId = (Long) record.get("user_id");
            Integer activeDays = ((Number) record.get("active_days")).intValue();
            activeDaysMap.put(userId, activeDays);
        }

        return activeDaysMap;
    }

    /**
     * 获取用户活跃情况（最近7天每天是否有饮食记录）
     */
    @GetMapping("/{id}/activity")
    public Result<Map<String, Object>> getUserActivity(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> weekData = new ArrayList<>();
        int activeDays = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int calorieTotal = dietRecordService.getDailyTotalCalories(id, date);
            boolean hasRecord = calorieTotal > 0;
            if (hasRecord) activeDays++;

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("hasRecord", hasRecord);
            dayData.put("calories", calorieTotal);
            weekData.add(dayData);
        }

        result.put("weekData", weekData);
        result.put("activeDays", activeDays);
        result.put("activeRate", (activeDays / 7.0) * 100);

        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<User> getUserDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 禁用/启用用户
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Integer status = body.get("status");
        user.setStatus(status);
        userService.updateById(user);

        return Result.success(null);
    }

    // ==================== AI 识别校准相关接口 ====================

    /**
     * 获取AI识别日志列表（分页）
     */
    @GetMapping("/ai-logs")
    public Result<Map<String, Object>> getAiLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String keyword) {

        Page<AiLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AiLog> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AiLog::getRawResult, keyword);
        }
        wrapper.orderByDesc(AiLog::getCreateTime);

        Page<AiLog> pageResult = aiLogMapper.selectPage(pageParam, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 校准AI识别匹配（更新匹配的食物ID）
     */
    @PutMapping("/ai-logs/{id}/calibrate")
    public Result<Void> calibrateAiLog(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AiLog log = aiLogMapper.selectById(id);
        if (log == null) {
            return Result.error("日志不存在");
        }

        String correctedFoodIds = body.get("foodIds");
        log.setMatchedFoodIds(correctedFoodIds);
        log.setIsCorrected(1);
        aiLogMapper.updateById(log);

        return Result.success(null);
    }
}
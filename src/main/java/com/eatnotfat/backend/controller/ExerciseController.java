package com.eatnotfat.backend.controller;

import com.eatnotfat.backend.dto.ExercisePlanRequest;
import com.eatnotfat.backend.dto.ExerciseRecordDTO;
import com.eatnotfat.backend.dto.ExerciseReminderDTO;
import com.eatnotfat.backend.entity.ExerciseLibrary;
import com.eatnotfat.backend.entity.ExerciseRecord;
import com.eatnotfat.backend.entity.ExerciseReminder;
import com.eatnotfat.backend.service.ExerciseService;
import com.eatnotfat.backend.service.QwenService;
import com.eatnotfat.backend.vo.ExerciseCalendarVO;
import com.eatnotfat.backend.vo.ExerciseDashboardVO;
import com.eatnotfat.backend.vo.ExerciseStatsVO;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercise")
public class ExerciseController {

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private QwenService qwenService;

    // ==================== 看板 ====================

    @GetMapping("/dashboard")
    public Result<ExerciseDashboardVO> getDashboard(@RequestParam Long userId) {
        ExerciseDashboardVO dashboard = exerciseService.getDashboard(userId);
        return Result.success(dashboard);
    }

    // ==================== AI运动规划 ====================

    @PostMapping("/plan/generate")
    public Result<ExerciseDashboardVO.ExercisePlanVO> generatePlan(@RequestParam Long userId) {
        // 1. 组装请求数据
        ExercisePlanRequest request = exerciseService.buildPlanRequest(userId);

        // 2. 调用AI生成
        ExerciseDashboardVO.ExercisePlanVO plan = qwenService.generateExercisePlan(request);

        // 3. 保存计划
        try {
            com.eatnotfat.backend.entity.ExercisePlan planEntity =
                    new com.eatnotfat.backend.entity.ExercisePlan();
            planEntity.setUserId(userId);
            planEntity.setPlanDate(java.time.LocalDate.now());
            planEntity.setPlanContent(com.alibaba.fastjson2.JSON.toJSONString(plan));
            planEntity.setStatus(0);

            // 更新或插入
            com.eatnotfat.backend.entity.ExercisePlan existing =
                    exerciseService.getPlanByDate(userId, java.time.LocalDate.now());
            if (existing != null) {
                existing.setPlanContent(com.alibaba.fastjson2.JSON.toJSONString(plan));
                exerciseService.updatePlan(existing);
            } else {
                exerciseService.insertPlan(planEntity);
            }
        } catch (Exception e) {
            System.err.println("保存运动计划失败: " + e.getMessage());
        }

        return Result.success(plan);
    }

    @GetMapping("/plan/today")
    public Result<ExerciseDashboardVO.ExercisePlanVO> getTodayPlan(@RequestParam Long userId) {
        com.eatnotfat.backend.entity.ExercisePlan plan =
                exerciseService.getPlanByDate(userId, java.time.LocalDate.now());
        if (plan != null && plan.getPlanContent() != null) {
            return Result.success(exerciseService.parsePlanFromJson(plan.getPlanContent()));
        }
        return Result.success(null);
    }

    // ==================== 运动记录 ====================

    @PostMapping("/record")
    public Result<ExerciseRecord> saveRecord(@RequestBody ExerciseRecordDTO dto) {
        ExerciseRecord record = exerciseService.saveRecord(dto);
        return Result.success(record);
    }

    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id, @RequestParam Long userId) {
        exerciseService.deleteRecord(id, userId);
        return Result.success(null);
    }

    @GetMapping("/records")
    public Result<List<ExerciseRecord>> getRecords(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(exerciseService.getRecords(userId, days));
    }

    // ==================== 日历 ====================

    @GetMapping("/calendar")
    public Result<ExerciseCalendarVO> getCalendar(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return Result.success(exerciseService.getCalendar(userId, year, month));
    }

    // ==================== 统计 ====================

    @GetMapping("/stats")
    public Result<ExerciseStatsVO> getStats(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(exerciseService.getStats(userId, days));
    }

    // ==================== 疲劳度 ====================

    @GetMapping("/fatigue")
    public Result<ExerciseDashboardVO.FatigueStatus> getFatigue(@RequestParam Long userId) {
        return Result.success(exerciseService.getFatigueStatus(userId));
    }

    // ==================== 运动库 ====================

    @GetMapping("/library")
    public Result<List<ExerciseLibrary>> getLibrary(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return Result.success(exerciseService.getExercisesByCategory(category));
        }
        return Result.success(exerciseService.getAllExercises());
    }

    // ==================== 提醒 ====================

    @GetMapping("/reminder")
    public Result<ExerciseReminder> getReminder(@RequestParam Long userId) {
        return Result.success(exerciseService.getReminder(userId));
    }

    @PostMapping("/reminder")
    public Result<ExerciseReminder> saveReminder(@RequestBody ExerciseReminderDTO dto) {
        return Result.success(exerciseService.saveReminder(dto));
    }
}

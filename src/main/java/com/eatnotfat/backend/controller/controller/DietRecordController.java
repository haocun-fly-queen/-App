package com.eatnotfat.backend.controller.controller;

import com.eatnotfat.backend.dto.SaveDietRecordDTO;
import com.eatnotfat.backend.entity.DietRecord;
import com.eatnotfat.backend.entity.DietRecordItem;
import com.eatnotfat.backend.service.DietRecordService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diet")
public class DietRecordController {

    @Autowired
    private DietRecordService dietRecordService;

    /**
     * 保存饮食记录
     */
    @PostMapping("/record")
    public Map<String, Object> saveRecord(@RequestBody SaveDietRecordDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("收到保存请求: userId=" + dto.getUserId() + ", mealType=" + dto.getMealType());
            System.out.println("食物数量: " + dto.getItems().size());

            // 转换 DTO 到实体
            List<DietRecordItem> items = new ArrayList<>();
            for (SaveDietRecordDTO.DietRecordItemDTO itemDTO : dto.getItems()) {
                DietRecordItem item = new DietRecordItem();
                item.setFoodType(itemDTO.getFoodType());
                item.setFoodId(itemDTO.getFoodId());
                item.setFoodName(itemDTO.getFoodName());
                item.setEatWeight(BigDecimal.valueOf(itemDTO.getEatWeight()));
                item.setCalorie(BigDecimal.valueOf(itemDTO.getCalorie()));
                item.setCarbs(itemDTO.getCarbs() != null ? BigDecimal.valueOf(itemDTO.getCarbs()) : null);
                item.setProtein(itemDTO.getProtein() != null ? BigDecimal.valueOf(itemDTO.getProtein()) : null);
                item.setFat(itemDTO.getFat() != null ? BigDecimal.valueOf(itemDTO.getFat()) : null);
                items.add(item);

                System.out.println("食物: " + itemDTO.getFoodName() + ", 热量: " + itemDTO.getCalorie());
            }

            DietRecord record = dietRecordService.saveDietRecord(
                    dto.getUserId(),
                    dto.getMealType(),
                    items,
                    dto.getRemark()
            );

            System.out.println("保存成功, recordId=" + record.getId());

            result.put("code", 200);
            result.put("message", "保存成功");
            result.put("data", record);
        } catch (Exception e) {
            System.err.println("保存失败: " + e.getMessage());
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 查询饮食记录（支持单日期或日期范围）
     */
    @GetMapping("/records")
    public Map<String, Object> getRecordsByDate(
            @RequestParam Long userId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DietRecord> records;

            if (date != null && !date.isEmpty()) {
                // 单日期查询
                LocalDate localDate = LocalDate.parse(date);
                records = dietRecordService.getRecordsByDate(userId, localDate);
                int totalCalories = dietRecordService.getDailyTotalCalories(userId, localDate);
                result.put("totalCalories", totalCalories);
            } else if (startDate != null && endDate != null) {
                // 日期范围查询
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                records = dietRecordService.getRecordsByDateRange(userId, start, end);
                int totalCalories = records.stream()
                        .mapToInt(r -> r.getTotalCalorie() != null ? r.getTotalCalorie().intValue() : 0)
                        .sum();
                result.put("totalCalories", totalCalories);
            } else {
                // 默认查询今天
                LocalDate today = LocalDate.now();
                records = dietRecordService.getRecordsByDate(userId, today);
                int totalCalories = dietRecordService.getDailyTotalCalories(userId, today);
                result.put("totalCalories", totalCalories);
            }

            result.put("code", 200);
            result.put("data", records);
        } catch (Exception e) {
            System.err.println("查询失败: " + e.getMessage());
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取某条记录的详情
     */
    @GetMapping("/record/{recordId}")
    public Map<String, Object> getRecordDetail(@PathVariable Long recordId) {
        Map<String, Object> result = new HashMap<>();
        try {
            DietRecord record = dietRecordService.getById(recordId);
            if (record == null) {
                result.put("code", 404);
                result.put("message", "记录不存在");
                return result;
            }

            List<DietRecordItem> items = dietRecordService.getRecordItems(recordId);

            result.put("code", 200);
            result.put("data", record);
            result.put("items", items);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除饮食记录
     */
    @DeleteMapping("/record/{recordId}")
    public Map<String, Object> deleteRecord(
            @PathVariable Long recordId,
            @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            dietRecordService.deleteDietRecord(recordId, userId);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取本周饮食数据
     */
    @GetMapping("/weekly")
    public Map<String, Object> getWeeklyData(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> weeklyData = dietRecordService.getWeeklyData(userId);
            result.put("code", 200);
            result.put("data", weeklyData);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取今日已摄入热量
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayCalories(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            int totalCalories = dietRecordService.getDailyTotalCalories(userId, LocalDate.now());
            result.put("code", 200);
            result.put("data", totalCalories);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取数据看板数据
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(6);

            Map<String, Object> data = dietRecordService.getDashboardData(userId, start, end);
            result.put("code", 200);
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    /**
     * 获取今日营养素摄入（碳水、蛋白质、脂肪）
     */
    @GetMapping("/today-nutrients")
    public Result<Map<String, Double>> getTodayNutrients(
            @RequestParam Long userId,
            @RequestParam String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            Map<String, Double> nutrients = dietRecordService.getTodayNutrients(userId, localDate);
            return Result.success(nutrients);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }
    /**
     * 获取用户第一次记录的日期
     */
    @GetMapping("/first-record-date")
    public Result<Map<String, String>> getFirstRecordDate(@RequestParam Long userId) {
        try {
            Map<String, String> result = dietRecordService.getFirstRecordDate(userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
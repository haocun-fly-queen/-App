package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.DietRecord;
import com.eatnotfat.backend.entity.DietRecordItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DietRecordService extends IService<DietRecord> {

    /**
     * 保存饮食记录（包含详情）
     */
    DietRecord saveDietRecord(Long userId, Integer mealType, List<DietRecordItem> items, String remark);

    /**
     * 查询用户某天的饮食记录
     */
    List<DietRecord> getRecordsByDate(Long userId, LocalDate date);

    /**
     * 查询用户某天的饮食详情
     */
    List<DietRecordItem> getRecordItems(Long recordId);

    /**
     * 查询用户某天的总热量
     */
    Integer getDailyTotalCalories(Long userId, LocalDate date);

    /**
     * 删除饮食记录
     */
    void deleteDietRecord(Long recordId, Long userId);
    /**
     * 查询日期范围内的饮食记录
     */
    List<DietRecord> getRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    /**
     * 获取用户本周的饮食数据
     */
    Map<String, Object> getWeeklyData(Long userId);
    /**
     * 获取今日营养素摄入
     */
    /**
     * 获取用户第一次记录的日期
     */
    Map<String, String> getFirstRecordDate(Long userId);
    Map<String, Double> getTodayNutrients(Long userId, LocalDate date);

    /**
     * 获取数据看板数据
     */
    Map<String, Object> getDashboardData(Long userId, LocalDate startDate, LocalDate endDate);
}
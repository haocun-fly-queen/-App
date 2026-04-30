package com.eatnotfat.backend.controller.controller;

import com.eatnotfat.backend.entity.WeightRecord;
import com.eatnotfat.backend.service.WeightRecordService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weight")
public class WeightRecordController {

    @Autowired
    private WeightRecordService weightRecordService;

    /**
     * 保存体重记录
     */
    @PostMapping("/record")
    public Result<WeightRecord> saveRecord(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            BigDecimal weight = new BigDecimal(params.get("weight").toString());
            LocalDate recordDate = params.get("recordDate") != null ?
                    LocalDate.parse(params.get("recordDate").toString()) : LocalDate.now();
            String remark = (String) params.get("remark");

            WeightRecord record = weightRecordService.saveWeightRecord(userId, weight, recordDate, remark);
            return Result.success(record);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取最新的体重
     */
    @GetMapping("/latest")
    public Result<WeightRecord> getLatestWeight(@RequestParam Long userId) {
        WeightRecord record = weightRecordService.getLatestWeight(userId);
        return Result.success(record);
    }

    /**
     * 获取最近N天的体重记录
     */
    @GetMapping("/recent")
    public Result<List<WeightRecord>> getRecentWeights(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "30") int days) {
        List<WeightRecord> records = weightRecordService.getRecentWeights(userId, days);
        return Result.success(records);
    }

    /**
     * 获取体重统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getWeightStats(@RequestParam Long userId) {
        Map<String, Object> stats = weightRecordService.getWeightStats(userId);
        return Result.success(stats);
    }

    /**
     * 删除体重记录
     */
    @DeleteMapping("/record/{recordId}")
    public Result<Void> deleteRecord(
            @PathVariable Long recordId,
            @RequestParam Long userId) {
        try {
            weightRecordService.deleteWeightRecord(recordId, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
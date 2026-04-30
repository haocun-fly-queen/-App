package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.entity.WeightRecord;
import com.eatnotfat.backend.mapper.WeightRecordMapper;
import com.eatnotfat.backend.service.UserService;
import com.eatnotfat.backend.service.WeightRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class WeightRecordServiceImpl extends ServiceImpl<WeightRecordMapper, WeightRecord> implements WeightRecordService {

    @Autowired
    private UserService userService;

    @Override
    public WeightRecord saveWeightRecord(Long userId, BigDecimal weight, LocalDate recordDate, String remark) {
        // 检查是否已存在当天的记录
        LambdaQueryWrapper<WeightRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecord::getUserId, userId)
                .eq(WeightRecord::getRecordDate, recordDate);
        WeightRecord existing = this.getOne(wrapper);

        if (existing != null) {
            // 更新已有记录
            existing.setWeight(weight);
            existing.setRemark(remark);
            this.updateById(existing);
            return existing;
        } else {
            // 新增记录
            WeightRecord record = new WeightRecord();
            record.setUserId(userId);
            record.setWeight(weight);
            record.setRecordDate(recordDate);
            record.setRemark(remark);
            this.save(record);
            return record;
        }
    }

    @Override
    public WeightRecord getLatestWeight(Long userId) {
        return this.baseMapper.selectLatest(userId);
    }

    @Override
    public List<WeightRecord> getRecentWeights(Long userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        return this.baseMapper.selectRecent(userId, startDate);
    }

    @Override
    public Map<String, Object> getWeightStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 获取所有体重记录
        List<WeightRecord> records = this.baseMapper.selectAllByUser(userId);

        if (records.isEmpty()) {
            stats.put("hasData", false);
            stats.put("currentWeight", null);
            stats.put("targetWeight", null);
            stats.put("difference", null);
            stats.put("trend", null);
            stats.put("minWeight", null);
            stats.put("maxWeight", null);
            stats.put("avgWeight", null);
            return stats;
        }

        // 当前体重（最新）
        WeightRecord latest = records.get(records.size() - 1);
        BigDecimal currentWeight = latest.getWeight();

        // 获取用户目标体重
        User user = userService.getById(userId);
        BigDecimal targetWeight = user != null ? user.getTargetWeight() : null;

        // 计算与目标体重的差距
        BigDecimal difference = null;
        if (targetWeight != null) {
            difference = currentWeight.subtract(targetWeight);
        }

        // 计算趋势（与7天前对比）
        String trend = "stable";
        if (records.size() >= 2) {
            BigDecimal sevenDaysAgo = null;
            LocalDate sevenDaysAgoDate = LocalDate.now().minusDays(7);
            for (WeightRecord record : records) {
                if (record.getRecordDate().isBefore(sevenDaysAgoDate) || record.getRecordDate().equals(sevenDaysAgoDate)) {
                    sevenDaysAgo = record.getWeight();
                    break;
                }
            }
            if (sevenDaysAgo != null) {
                if (currentWeight.compareTo(sevenDaysAgo) < 0) {
                    trend = "down";
                } else if (currentWeight.compareTo(sevenDaysAgo) > 0) {
                    trend = "up";
                }
            }
        }

        // 统计信息
        BigDecimal minWeight = records.stream()
                .map(WeightRecord::getWeight)
                .min(BigDecimal::compareTo)
                .orElse(null);
        BigDecimal maxWeight = records.stream()
                .map(WeightRecord::getWeight)
                .max(BigDecimal::compareTo)
                .orElse(null);
        double avgWeight = records.stream()
                .mapToDouble(w -> w.getWeight().doubleValue())
                .average()
                .orElse(0);

        stats.put("hasData", true);
        stats.put("currentWeight", currentWeight);
        stats.put("targetWeight", targetWeight);
        stats.put("difference", difference);
        stats.put("trend", trend);
        stats.put("minWeight", minWeight);
        stats.put("maxWeight", maxWeight);
        stats.put("avgWeight", new BigDecimal(avgWeight).setScale(1, RoundingMode.HALF_UP));
        stats.put("recordCount", records.size());

        return stats;
    }

    @Override
    public void deleteWeightRecord(Long recordId, Long userId) {
        WeightRecord record = this.getById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        this.removeById(recordId);
    }
}
package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.WeightRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface WeightRecordService extends IService<WeightRecord> {

    /**
     * 保存体重记录
     */
    WeightRecord saveWeightRecord(Long userId, BigDecimal weight, LocalDate recordDate, String remark);

    /**
     * 获取用户最新的体重
     */
    WeightRecord getLatestWeight(Long userId);

    /**
     * 获取用户最近N天的体重记录
     */
    List<WeightRecord> getRecentWeights(Long userId, int days);

    /**
     * 获取体重统计数据
     */
    Map<String, Object> getWeightStats(Long userId);

    /**
     * 删除体重记录
     */
    void deleteWeightRecord(Long recordId, Long userId);
}
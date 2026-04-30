package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.WeightRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecord> {

    /**
     * 获取用户最新的体重记录
     */
    @Select("SELECT * FROM eat_not_fat_weight_record WHERE user_id = #{userId} ORDER BY record_date DESC LIMIT 1")
    WeightRecord selectLatest(Long userId);

    /**
     * 获取用户最近N天的体重记录
     */
    @Select("SELECT * FROM eat_not_fat_weight_record WHERE user_id = #{userId} AND record_date >= #{startDate} ORDER BY record_date ASC")
    List<WeightRecord> selectRecent(Long userId, LocalDate startDate);

    /**
     * 获取用户的体重变化趋势（最近30天）
     */
    @Select("SELECT * FROM eat_not_fat_weight_record WHERE user_id = #{userId} ORDER BY record_date ASC")
    List<WeightRecord> selectAllByUser(Long userId);
}
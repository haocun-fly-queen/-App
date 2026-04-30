package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.DietRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DietRecordMapper extends BaseMapper<DietRecord> {

    /**
     * 查询用户某天的饮食记录
     */
    @Select("SELECT * FROM eat_not_fat_diet_record WHERE user_id = #{userId} AND DATE(meal_time) = #{date} ORDER BY meal_time DESC")
    List<DietRecord> selectByDate(Long userId, String date);

    /**
     * 查询用户某月的饮食记录
     */
    @Select("SELECT * FROM eat_not_fat_diet_record WHERE user_id = #{userId} AND DATE_FORMAT(meal_time, '%Y-%m') = #{month} ORDER BY meal_time DESC")
    List<DietRecord> selectByMonth(Long userId, String month);

    /**
     * 查询时间段内的饮食记录
     */
    @Select("SELECT * FROM eat_not_fat_diet_record WHERE user_id = #{userId} AND meal_time BETWEEN #{startTime} AND #{endTime} ORDER BY meal_time DESC")
    List<DietRecord> selectByTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
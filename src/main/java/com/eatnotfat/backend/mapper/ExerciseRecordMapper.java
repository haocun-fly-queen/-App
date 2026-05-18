package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExerciseRecordMapper extends BaseMapper<ExerciseRecord> {

    @Select("SELECT * FROM exercise_record WHERE user_id = #{userId} AND record_time >= #{startTime} ORDER BY record_time DESC")
    List<ExerciseRecord> selectByDateRange(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    @Select("SELECT * FROM exercise_record WHERE user_id = #{userId} AND DATE(record_time) = #{date} ORDER BY record_time")
    List<ExerciseRecord> selectByDate(@Param("userId") Long userId, @Param("date") String date);

    @Select("SELECT * FROM exercise_record WHERE user_id = #{userId} ORDER BY record_time DESC LIMIT #{limit}")
    List<ExerciseRecord> selectRecent(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_record WHERE user_id = #{userId} AND DATE(record_time) = #{date}")
    Double sumCaloriesByDate(@Param("userId") Long userId, @Param("date") String date);

    @Select("SELECT COUNT(DISTINCT DATE(record_time)) FROM exercise_record WHERE user_id = #{userId} AND record_time >= #{startTime}")
    int countActiveDays(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}

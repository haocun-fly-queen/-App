package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.ExercisePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface ExercisePlanMapper extends BaseMapper<ExercisePlan> {

    @Select("SELECT * FROM exercise_plan WHERE user_id = #{userId} AND plan_date = #{date}")
    ExercisePlan selectByDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}

package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.ExerciseReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExerciseReminderMapper extends BaseMapper<ExerciseReminder> {

    @Select("SELECT * FROM exercise_reminder WHERE user_id = #{userId}")
    ExerciseReminder selectByUserId(Long userId);
}

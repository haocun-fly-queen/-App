package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.ExerciseLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExerciseLibraryMapper extends BaseMapper<ExerciseLibrary> {

    @Select("SELECT * FROM exercise_library WHERE is_enabled = 1 ORDER BY category, difficulty")
    List<ExerciseLibrary> selectAllEnabled();

    @Select("SELECT * FROM exercise_library WHERE is_enabled = 1 AND category = #{category} ORDER BY difficulty")
    List<ExerciseLibrary> selectByCategory(String category);

    @Select("SELECT * FROM exercise_library WHERE is_enabled = 1 AND suitable_goal IN ('all', #{goalType}) ORDER BY difficulty")
    List<ExerciseLibrary> selectByGoal(String goalType);
}

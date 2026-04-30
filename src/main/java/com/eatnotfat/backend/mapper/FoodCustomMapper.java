package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.FoodCustom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FoodCustomMapper extends BaseMapper<FoodCustom> {

    @Select("SELECT * FROM eat_not_fat_food_custom WHERE user_id = #{userId} AND status = 1 ORDER BY create_time DESC")
    List<FoodCustom> selectByUser(Long userId);
}
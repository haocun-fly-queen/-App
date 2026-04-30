package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.FoodStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FoodStandardMapper extends BaseMapper<FoodStandard> {

    /**
     * 根据关键词搜索食物（名称或别名）
     */
    @Select("SELECT * FROM eat_not_fat_food WHERE name LIKE CONCAT('%', #{keyword}, '%') OR alias LIKE CONCAT('%', #{keyword}, '%') AND status = 1")
    List<FoodStandard> searchByName(String keyword);

    /**
     * 根据分类查询食物
     */
    @Select("SELECT * FROM eat_not_fat_food WHERE category = #{category} AND status = 1")
    List<FoodStandard> selectByCategory(String category);

    /**
     * 获取所有启用的食物
     */
    @Select("SELECT * FROM eat_not_fat_food WHERE status = 1 ORDER BY category, name")
    List<FoodStandard> selectAllEnabled();
}
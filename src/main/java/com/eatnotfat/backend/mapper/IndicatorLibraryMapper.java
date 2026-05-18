package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.IndicatorLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface IndicatorLibraryMapper extends BaseMapper<IndicatorLibrary> {

    @Select("SELECT * FROM indicator_library WHERE category = #{category} AND is_enabled = 1")
    List<IndicatorLibrary> selectByCategory(@Param("category") String category);

    @Select("SELECT * FROM indicator_library WHERE code = #{code} AND is_enabled = 1")
    IndicatorLibrary selectByCode(@Param("code") String code);

    @Select("SELECT * FROM indicator_library WHERE is_enabled = 1")
    List<IndicatorLibrary> selectAllEnabled();
}

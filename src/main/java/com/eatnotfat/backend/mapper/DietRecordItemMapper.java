package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.DietRecordItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DietRecordItemMapper extends BaseMapper<DietRecordItem> {

    /**
     * 根据记录ID查询详情
     */
    @Select("SELECT * FROM eat_not_fat_diet_item WHERE record_id = #{recordId} ORDER BY sort ASC")
    List<DietRecordItem> selectByRecordId(Long recordId);
}
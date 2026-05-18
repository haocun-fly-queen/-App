package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.HealthIndicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface HealthIndicatorMapper extends BaseMapper<HealthIndicator> {

    @Select("SELECT hi.*, hr.report_date FROM health_indicator hi " +
            "JOIN health_report hr ON hi.report_id = hr.id " +
            "WHERE hi.user_id = #{userId} AND hi.indicator_code = #{code} " +
            "AND hr.status = 2 ORDER BY hr.report_date ASC")
    List<HealthIndicator> selectTrendByUserAndCode(@Param("userId") Long userId,
                                                   @Param("code") String code);

    @Select("SELECT hi.* FROM health_indicator hi " +
            "JOIN health_report hr ON hi.report_id = hr.id " +
            "WHERE hi.user_id = #{userId} AND hr.status = 2 AND hi.status >= 2 " +
            "ORDER BY hi.status DESC")
    List<HealthIndicator> selectLatestAbnormalIndicators(@Param("userId") Long userId);
}

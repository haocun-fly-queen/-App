package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.HealthReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthReportMapper extends BaseMapper<HealthReport> {
}

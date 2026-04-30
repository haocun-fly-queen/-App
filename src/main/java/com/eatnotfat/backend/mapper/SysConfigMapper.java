package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置键获取配置值
     */
    @Select("SELECT config_value FROM eat_not_fat_sys_config WHERE config_key = #{key} AND status = 1")
    String getValueByKey(@Param("key") String key);

    /**
     * 获取所有启用的配置
     */
    @Select("SELECT * FROM eat_not_fat_sys_config WHERE status = 1")
    List<SysConfig> getAllEnabled();
}
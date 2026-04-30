package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.SysConfig;

import java.util.List;

/**
 * 系统配置 Service 接口
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据配置键获取配置值
     * @param key 配置键
     * @return 配置值
     */
    String getValue(String key);
    /**
     * 获取所有启用的配置
     * @return 配置列表
     */
    List<SysConfig> getAllEnabled();
    /**
     * 设置配置值
     * @param key 配置键
     * @param value 配置值
     */
    void setValue(String key, String value);
}
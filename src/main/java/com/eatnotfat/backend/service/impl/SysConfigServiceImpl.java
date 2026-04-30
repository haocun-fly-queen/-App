package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.SysConfig;
import com.eatnotfat.backend.mapper.SysConfigMapper;
import com.eatnotfat.backend.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置 Service 实现类
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {
    @Override
    public List<SysConfig> getAllEnabled() {
        return this.baseMapper.getAllEnabled();
    }
    /**
     * 根据配置键获取配置值
     * @param key 配置键
     * @return 配置值
     */
    @Override
    public String getValue(String key) {
        // 直接调用 Mapper 的自定义方法
        return this.baseMapper.getValueByKey(key);
    }

    /**
     * 设置配置值（如果存在则更新，不存在则新增）
     * @param key 配置键
     * @param value 配置值
     */
    @Override
    public void setValue(String key, String value) {
        // 查询是否存在
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig config = this.getOne(wrapper);

        if (config != null) {
            // 存在则更新
            config.setConfigValue(value);
            this.updateById(config);
        } else {
            // 不存在则新增
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            this.save(config);
        }
    }
}
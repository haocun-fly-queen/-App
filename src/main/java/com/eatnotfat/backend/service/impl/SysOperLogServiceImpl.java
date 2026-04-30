package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.SysOperLog;
import com.eatnotfat.backend.mapper.SysOperLogMapper;
import com.eatnotfat.backend.service.SysOperLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public void recordLog(SysOperLog log) {
        log.setCreateTime(LocalDateTime.now());
        this.save(log);
    }

    @Override
    public void cleanLog() {
        // 保留最近30天的日志，删除30天前的
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysOperLog::getCreateTime, thirtyDaysAgo);
        this.remove(wrapper);
    }
}
package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.SysOperLog;

public interface SysOperLogService extends IService<SysOperLog> {

    /**
     * 记录操作日志
     */
    void recordLog(SysOperLog log);

    /**
     * 清空日志（保留最近30天）
     */
    void cleanLog();
}
package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.OperLog;

/**
 * 操作日志 Service 接口
 */
public interface OperLogService extends IService<OperLog> {

    /**
     * 记录操作日志
     * @param adminId 操作人ID
     * @param module 操作模块
     * @param operation 操作类型
     * @param method 请求方法
     * @param params 请求参数
     * @param result 返回结果
     * @param ip IP地址
     * @param costTime 耗时
     */
    void record(Long adminId, String module, String operation,
                String method, String params, String result,
                String ip, Integer costTime);
}
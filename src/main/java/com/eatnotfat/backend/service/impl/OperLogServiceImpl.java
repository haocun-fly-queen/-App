package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.OperLog;
import com.eatnotfat.backend.mapper.OperLogMapper;
import com.eatnotfat.backend.service.OperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志 Service 实现类
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLog> implements OperLogService {

    /**
     * 记录操作日志
     */
    @Override
    public void record(Long adminId, String module, String operation,
                       String method, String params, String result,
                       String ip, Integer costTime) {
        // 创建日志对象
        OperLog log = new OperLog();
        log.setAdminId(adminId);
        log.setModule(module);
        log.setOperation(operation);
        log.setMethod(method);
        log.setParams(params);
        log.setResult(result);
        log.setIp(ip);
        log.setCostTime(costTime);

        // 保存到数据库
        this.save(log);
    }
}
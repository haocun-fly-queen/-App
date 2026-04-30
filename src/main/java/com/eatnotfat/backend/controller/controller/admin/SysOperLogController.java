package com.eatnotfat.backend.controller.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatnotfat.backend.entity.SysOperLog;
import com.eatnotfat.backend.service.SysOperLogService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/operlog")
public class SysOperLogController {

    @Autowired
    private SysOperLogService sysOperLogService;

    /**
     * 获取操作日志列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();

        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysOperLog::getModule, module);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysOperLog::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(SysOperLog::getStatus, status);
        }

        wrapper.orderByDesc(SysOperLog::getCreateTime);

        Page<SysOperLog> pageResult = sysOperLogService.page(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 获取日志详情
     */
    @GetMapping("/{id}")
    public Result<SysOperLog> getLogDetail(@PathVariable Long id) {
        SysOperLog log = sysOperLogService.getById(id);
        return Result.success(log);
    }

    /**
     * 清空日志
     */
    @DeleteMapping("/clean")
    public Result<Void> cleanLog() {
        sysOperLogService.cleanLog();
        return Result.success(null);
    }
}
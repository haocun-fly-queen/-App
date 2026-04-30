package com.eatnotfat.backend.controller.controller.admin;

import com.eatnotfat.backend.entity.SysConfig;
import com.eatnotfat.backend.service.SysConfigService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/config")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 获取所有配置列表
     */
    @GetMapping("/list")
    public Result<List<SysConfig>> getAllConfigs() {
        List<SysConfig> configs = sysConfigService.getAllEnabled();
        return Result.success(configs);
    }

    /**
     * 获取单个配置值
     */
    @GetMapping("/{key}")
    public Result<String> getConfig(@PathVariable String key) {
        String value = sysConfigService.getValue(key);
        return Result.success(value);
    }

    /**
     * 更新配置
     */
    @PutMapping("/{key}")
    public Result<Void> updateConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        sysConfigService.setValue(key, value);
        return Result.success(null);
    }
}
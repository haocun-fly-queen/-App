package com.eatnotfat.backend.controller.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatnotfat.backend.entity.UserLoginLog;
import com.eatnotfat.backend.service.UserLoginLogService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/login-log")
public class UserLoginLogController {

    @Autowired
    private UserLoginLogService userLoginLogService;

    @GetMapping("/list")
    public Result<Map<String, Object>> getLoginLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) Integer loginStatus) {

        LambdaQueryWrapper<UserLoginLog> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(UserLoginLog::getPhone, keyword)
                    .or()
                    .like(UserLoginLog::getNickname, keyword)
                    .or()
                    .like(UserLoginLog::getOpenid, keyword);
        }

        if (loginType != null && !loginType.isEmpty()) {
            wrapper.eq(UserLoginLog::getLoginType, loginType);
        }

        if (loginStatus != null) {
            wrapper.eq(UserLoginLog::getLoginStatus, loginStatus);
        }

        wrapper.orderByDesc(UserLoginLog::getLoginTime);

        Page<UserLoginLog> pageResult = userLoginLogService.page(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }
}
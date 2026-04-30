package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.entity.UserLoginLog;
import jakarta.servlet.http.HttpServletRequest;

public interface UserLoginLogService extends IService<UserLoginLog> {

    void recordLoginSuccess(User user, HttpServletRequest request, String loginType);

    // 新增：支持传入昵称
    void recordLoginSuccess(User user, HttpServletRequest request, String loginType, String loginNickname);

    void recordLoginFailure(String phone, String failReason, HttpServletRequest request, String loginType);
}
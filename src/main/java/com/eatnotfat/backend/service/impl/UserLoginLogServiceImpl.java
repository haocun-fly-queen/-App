package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.entity.UserLoginLog;
import com.eatnotfat.backend.mapper.UserLoginLogMapper;
import com.eatnotfat.backend.service.UserLoginLogService;
import com.eatnotfat.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserLoginLogServiceImpl extends ServiceImpl<UserLoginLogMapper, UserLoginLog> implements UserLoginLogService {

    @Autowired
    private UserService userService;

    @Override
    public void recordLoginSuccess(User user, HttpServletRequest request, String loginType) {
        recordLoginSuccess(user, request, loginType, null);
    }

    @Override
    public void recordLoginSuccess(User user, HttpServletRequest request, String loginType, String loginNickname) {
        UserLoginLog log = new UserLoginLog();

        log.setUserId(user.getId());
        log.setOpenid(user.getWxOpenid());
        log.setPhone(user.getPhone());

        // 决定使用哪个昵称
        String finalNickname;
        if (loginNickname != null && !loginNickname.isEmpty()) {
            finalNickname = loginNickname;
            // 如果昵称变了，更新用户表
            if (!loginNickname.equals(user.getNickname())) {
                user.setNickname(loginNickname);
                userService.updateById(user);
            }
        } else {
            finalNickname = user.getNickname();
        }

        log.setNickname(finalNickname);
        log.setLoginTime(LocalDateTime.now());
        log.setLoginType(loginType);
        log.setLoginStatus(1);
        log.setLoginIp(getIpAddress(request));

        this.save(log);
    }

    @Override
    public void recordLoginFailure(String phone, String failReason, HttpServletRequest request, String loginType) {
        UserLoginLog log = new UserLoginLog();

        log.setPhone(phone);
        log.setLoginTime(LocalDateTime.now());
        log.setLoginType(loginType);
        log.setLoginStatus(0);
        log.setFailReason(failReason);
        log.setLoginIp(getIpAddress(request));

        this.save(log);
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
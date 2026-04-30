package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.Admin;
import com.eatnotfat.backend.mapper.AdminMapper;
import com.eatnotfat.backend.service.AdminService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Admin login(String username, String password) {
        System.out.println("========== 登录方法被调用 ==========");
        System.out.println("传入的用户名: [" + username + "]");
        System.out.println("传入的密码: [" + password + "]");

        // 查询用户
        Admin admin = this.baseMapper.selectByUsername(username);

        if (admin == null) {
            System.out.println("查询结果: 用户不存在");
            return null;
        }

        System.out.println("查询结果: 用户存在");
        System.out.println("用户ID: " + admin.getId());
        System.out.println("数据库中的密码: [" + admin.getPassword() + "]");
        System.out.println("数据库密码长度: " + admin.getPassword().length());

        // 验证密码
        boolean matches = passwordEncoder.matches(password, admin.getPassword());
        System.out.println("密码匹配结果: " + matches);

        if (!matches) {
            System.out.println("密码错误，登录失败");
            return null;
        }

        System.out.println("密码正确，登录成功");
        return admin;
    }

    @Override
    public void updateLoginInfo(Long id, String ip) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setLastLoginTime(LocalDateTime.now());
        admin.setLastLoginIp(ip);
        this.updateById(admin);
    }

    // ========== 新增方法（用于管理员管理）==========

    @Override
    public Admin getByUsername(String username) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public boolean isUsernameExist(String username, Long excludeId) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(Admin::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }
}
package com.eatnotfat.backend.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.UserMapper;
import com.eatnotfat.backend.service.EmailService;
import com.eatnotfat.backend.service.UserService;
import com.eatnotfat.backend.utils.TokenUtil;
import com.eatnotfat.backend.utils.WeChatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private WeChatUtil weChatUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ===================== 微信登录 =====================
    @Override
    public Map<String, Object> loginByWechat(String code, String encryptedData, String iv,
                                             String nickName, String avatarUrl, Boolean silent) {

        System.out.println("========== 微信登录开始 ==========");
        System.out.println("接收到的 code: " + code);

        Map<String, String> wxData = weChatUtil.getOpenIdAndSessionKey(code);
        String openid = wxData.get("openid");
        String sessionKey = wxData.get("session_key");

        System.out.println("微信返回的 openid: " + openid);
        System.out.println("微信返回的 session_key: " + sessionKey);

        if (openid == null || openid.isEmpty()) {
            System.err.println("获取 openid 失败！请检查 code 是否有效");
            throw new RuntimeException("微信登录失败，无法获取用户信息");
        }

        String finalNickName = "微信用户";
        String finalAvatarUrl = null;

        if (encryptedData != null && iv != null && !Boolean.TRUE.equals(silent)) {
            try {
                WeChatUtil.UserInfo userInfo = weChatUtil.decryptUserInfo(encryptedData, iv, sessionKey);
                finalNickName = userInfo.getNickName();
                finalAvatarUrl = userInfo.getAvatarUrl();
                System.out.println("解密获取到昵称: " + finalNickName);
            } catch (Exception e) {
                System.err.println("解密用户信息失败: " + e.getMessage());
                if (nickName != null) finalNickName = nickName;
                if (avatarUrl != null) finalAvatarUrl = avatarUrl;
            }
        } else {
            if (nickName != null) finalNickName = nickName;
            if (avatarUrl != null) finalAvatarUrl = avatarUrl;
        }

        System.out.println("根据 openid 查询用户: " + openid);
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getWxOpenid, openid));

        if (user == null) {
            System.out.println("用户不存在，创建新用户");
            user = new User();
            user.setWxOpenid(openid);
            user.setNickname(finalNickName);
            user.setAvatarUrl(finalAvatarUrl);
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            this.save(user);
            System.out.println("新用户创建成功，ID: " + user.getId());
        } else {
            System.out.println("用户已存在，ID: " + user.getId());
            if (!"微信用户".equals(finalNickName) && finalNickName != null) {
                user.setNickname(finalNickName);
                System.out.println("更新昵称为: " + finalNickName);
            }
            if (finalAvatarUrl != null) {
                user.setAvatarUrl(finalAvatarUrl);
                System.out.println("更新头像");
            }
            user.setUpdateTime(LocalDateTime.now());
            this.updateById(user);
            System.out.println("用户信息更新完成");
        }

        String token = tokenUtil.generateToken(user.getId());
        System.out.println("生成 Token，用户ID: " + user.getId());
        System.out.println("========== 微信登录结束 ==========");

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("token", token);
        result.put("height", user.getHeight());
        result.put("currentWeight", user.getCurrentWeight());

        return result;
    }

    // ===================== 手机号登录 =====================
    @Override
    public Map<String, Object> loginByPhone(String phone, String verifyCode) {
        System.out.println("========== 手机号登录开始 ==========");
        System.out.println("手机号: " + phone);

        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));

        if (user == null) {
            System.out.println("用户不存在，创建新用户");
            user = new User();
            user.setPhone(phone);
            user.setNickname("手机用户");
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            this.save(user);
            System.out.println("新用户创建成功，ID: " + user.getId());
        } else {
            System.out.println("用户已存在，ID: " + user.getId());
        }

        String token = tokenUtil.generateToken(user.getId());
        System.out.println("生成 Token，用户ID: " + user.getId());
        System.out.println("========== 手机号登录结束 ==========");

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("token", token);
        result.put("height", user.getHeight());
        result.put("currentWeight", user.getCurrentWeight());

        return result;
    }

    // ===================== 检查账号是否可用 =====================
    @Override
    public boolean isUsernameAvailable(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.count(wrapper) == 0;
    }

    // ===================== 账号密码注册 =====================
    @Override
    public Map<String, Object> register(String username, String password, String phone, String verifyCode, String realCode) {
        System.out.println("========== 注册开始 ==========");
        System.out.println("账号: " + username + " | 手机号: " + phone);

        // 校验验证码
        if (realCode == null || !realCode.equals(verifyCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 校验账号是否已存在
        if (!isUsernameAvailable(username)) {
            throw new RuntimeException("该账号已被注册");
        }

        // 校验手机号是否已注册
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, phone);
        if (this.count(phoneWrapper) > 0) {
            throw new RuntimeException("该手机号已注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setNickname("用户");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        this.save(user);

        System.out.println("注册成功，用户ID: " + user.getId());
        System.out.println("========== 注册结束 ==========");

        // 生成 Token
        String token = tokenUtil.generateToken(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("username", user.getUsername());
        result.put("token", token);
        return result;
    }

    // ===================== 账号密码登录 =====================
    @Override
    public Map<String, Object> loginByPassword(String username, String password) {
        System.out.println("========== 账号密码登录开始 ==========");
        System.out.println("账号: " + username);

        // 根据账号查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);

        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 校验密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        // 生成 Token
        String token = tokenUtil.generateToken(user.getId());
        System.out.println("登录成功，用户ID: " + user.getId());
        System.out.println("========== 账号密码登录结束 ==========");

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("username", user.getUsername());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("token", token);
        result.put("height", user.getHeight());
        result.put("currentWeight", user.getCurrentWeight());
        return result;
    }

    // ===================== 更新用户档案 =====================
    @Override
    public void updateProfile(Long userId, User updateUser) {
        System.out.println("========== 更新用户档案开始 ==========");
        System.out.println("用户ID: " + userId);

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (updateUser.getNickname() != null) {
            user.setNickname(updateUser.getNickname());
            System.out.println("更新昵称: " + updateUser.getNickname());
        }
        if (updateUser.getAvatarUrl() != null) user.setAvatarUrl(updateUser.getAvatarUrl());
        if (updateUser.getGender() != null) user.setGender(updateUser.getGender());
        if (updateUser.getAge() != null) user.setAge(updateUser.getAge());
        if (updateUser.getHeight() != null) user.setHeight(updateUser.getHeight());
        if (updateUser.getCurrentWeight() != null) user.setCurrentWeight(updateUser.getCurrentWeight());
        if (updateUser.getTargetWeight() != null) user.setTargetWeight(updateUser.getTargetWeight());
        if (updateUser.getActivityLevel() != null) user.setActivityLevel(updateUser.getActivityLevel());
        if (updateUser.getGoalType() != null) user.setGoalType(updateUser.getGoalType());

        Integer calorieGoal = calculateDailyCalorieGoal(user);
        user.setDailyCalorieGoal(calorieGoal);
        System.out.println("计算热量目标: " + calorieGoal);

        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        System.out.println("用户档案更新完成");
        System.out.println("========== 更新用户档案结束 ==========");
    }

    // ===================== 计算每日热量目标 =====================
    @Override
    public Integer calculateDailyCalorieGoal(User user) {
        if (user.getHeight() == null || user.getCurrentWeight() == null ||
                user.getHeight().doubleValue() <= 0 || user.getCurrentWeight().doubleValue() <= 0) {
            System.out.println("身高或体重数据无效，使用默认值2000");
            return 2000;
        }

        int age = user.getAge() != null ? user.getAge() : 25;
        if (age <= 0) age = 25;

        double bmr;
        if (user.getGender() != null && user.getGender() == 1) {
            bmr = 10 * user.getCurrentWeight().doubleValue()
                    + 6.25 * user.getHeight().doubleValue()
                    - 5 * age
                    + 5;
        } else {
            bmr = 10 * user.getCurrentWeight().doubleValue()
                    + 6.25 * user.getHeight().doubleValue()
                    - 5 * age
                    - 161;
        }

        double activityFactor = 1.2;
        if (user.getActivityLevel() != null) {
            switch (user.getActivityLevel()) {
                case 2: activityFactor = 1.375; break;
                case 3: activityFactor = 1.55; break;
                case 4: activityFactor = 1.725; break;
                case 5: activityFactor = 1.9; break;
            }
        }

        double tdee = bmr * activityFactor;

        if (user.getGoalType() != null) {
            if (user.getGoalType() == 1) {
                tdee = tdee * 0.8;
            } else if (user.getGoalType() == 2) {
                tdee = tdee * 1.1;
            }
        }

        int result = (int) Math.round(tdee);

        if (result < 800) {
            System.out.println("计算结果异常(" + result + ")，使用默认值2000");
            return 2000;
        }

        if (result > 4000) {
            System.out.println("计算结果异常(" + result + ")，使用默认值2000");
            return 2000;
        }

        return result;
    }
    @Autowired
    private EmailService emailService;

    @Override
    public Map<String, Object> loginByEmail(String email, String verifyCode) {
        System.out.println("========== 邮箱验证码登录开始 ==========");
        System.out.println("邮箱: " + email);

        // 校验验证码
        if (!emailService.verifyCode(email, verifyCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));

        if (user == null) {
            // 新用户，自动创建
            user = new User();
            user.setEmail(email);
            user.setNickname("邮箱用户");
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            this.save(user);
            System.out.println("新用户创建成功，ID: " + user.getId());
        }

        String token = tokenUtil.generateToken(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("token", token);
        result.put("height", user.getHeight());
        result.put("currentWeight", user.getCurrentWeight());
        return result;
    }

    @Override
    public Map<String, Object> registerByEmail(String email, String password, String verifyCode) {
        System.out.println("========== 邮箱注册开始 ==========");

        // 校验验证码
        if (!emailService.verifyCode(email, verifyCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 校验邮箱是否已注册
        if (isEmailRegistered(email)) {
            throw new RuntimeException("该邮箱已注册");
        }

        // 创建用户
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname("用户");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        this.save(user);

        String token = tokenUtil.generateToken(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("token", token);
        return result;
    }

    @Override
    public Map<String, Object> loginByEmailPassword(String email, String password) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));

        if (user == null) {
            throw new RuntimeException("邮箱未注册");
        }

        if (user.getPasswordHash() == null) {
            throw new RuntimeException("该账号未设置密码，请使用验证码登录");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        String token = tokenUtil.generateToken(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("token", token);
        result.put("height", user.getHeight());
        result.put("currentWeight", user.getCurrentWeight());
        return result;
    }

    @Override
    public boolean isEmailRegistered(String email) {
        return this.count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0;
    }

}

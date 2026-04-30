package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.UserMapper;
import com.eatnotfat.backend.service.UserService;
import com.eatnotfat.backend.utils.TokenUtil;
import com.eatnotfat.backend.utils.WeChatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public Map<String, Object> loginByWechat(String code, String encryptedData, String iv,
                                             String nickName, String avatarUrl, Boolean silent) {

        System.out.println("========== 微信登录开始 ==========");
        System.out.println("接收到的 code: " + code);

        // 1. 调用微信接口获取 openid 和 session_key
        Map<String, String> wxData = weChatUtil.getOpenIdAndSessionKey(code);
        String openid = wxData.get("openid");
        String sessionKey = wxData.get("session_key");

        System.out.println("微信返回的 openid: " + openid);
        System.out.println("微信返回的 session_key: " + sessionKey);

        if (openid == null || openid.isEmpty()) {
            System.err.println("获取 openid 失败！请检查 code 是否有效");
            throw new RuntimeException("微信登录失败，无法获取用户信息");
        }

        // 2. 处理用户信息
        String finalNickName = "微信用户";
        String finalAvatarUrl = null;

        // 如果有加密数据且不是静默登录，解密获取用户信息
        if (encryptedData != null && iv != null && !Boolean.TRUE.equals(silent)) {
            try {
                WeChatUtil.UserInfo userInfo = weChatUtil.decryptUserInfo(encryptedData, iv, sessionKey);
                finalNickName = userInfo.getNickName();
                finalAvatarUrl = userInfo.getAvatarUrl();
                System.out.println("解密获取到昵称: " + finalNickName);
            } catch (Exception e) {
                System.err.println("解密用户信息失败: " + e.getMessage());
                // 如果解密失败，使用前端传递的未加密数据
                if (nickName != null) finalNickName = nickName;
                if (avatarUrl != null) finalAvatarUrl = avatarUrl;
            }
        } else {
            // 静默登录或没有加密数据，使用前端传递的数据
            if (nickName != null) finalNickName = nickName;
            if (avatarUrl != null) finalAvatarUrl = avatarUrl;
        }

        // 3. 查询或创建用户
        System.out.println("根据 openid 查询用户: " + openid);
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getWxOpenid, openid));

        if (user == null) {
            // 新用户，创建账号
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
            // 更新用户信息（如果昵称不是默认的）
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

        // 4. 生成 Token
        String token = tokenUtil.generateToken(user.getId());
        System.out.println("生成 Token，用户ID: " + user.getId());
        System.out.println("========== 微信登录结束 ==========");

        // 5. 返回结果（包含身高体重）
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
    public Map<String, Object> loginByPhone(String phone, String verifyCode) {
        System.out.println("========== 手机号登录开始 ==========");
        System.out.println("手机号: " + phone);

        // 查询用户是否存在
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));

        if (user == null) {
            // 新用户，创建账号
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

        // 生成 Token
        String token = tokenUtil.generateToken(user.getId());
        System.out.println("生成 Token，用户ID: " + user.getId());
        System.out.println("========== 手机号登录结束 ==========");

        // 返回结果（包含身高体重）
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
    public void updateProfile(Long userId, User updateUser) {
        System.out.println("========== 更新用户档案开始 ==========");
        System.out.println("用户ID: " + userId);

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新基本信息
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

        // 重新计算每日热量目标
        Integer calorieGoal = calculateDailyCalorieGoal(user);
        user.setDailyCalorieGoal(calorieGoal);
        System.out.println("计算热量目标: " + calorieGoal);

        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        System.out.println("用户档案更新完成");
        System.out.println("========== 更新用户档案结束 ==========");
    }

    @Override
    public Integer calculateDailyCalorieGoal(User user) {
        // 检查数据有效性
        if (user.getHeight() == null || user.getCurrentWeight() == null ||
                user.getHeight().doubleValue() <= 0 || user.getCurrentWeight().doubleValue() <= 0) {
            System.out.println("身高或体重数据无效，使用默认值2000");
            return 2000;
        }

        // 检查年龄
        int age = user.getAge() != null ? user.getAge() : 25;
        if (age <= 0) age = 25;

        // Mifflin-St Jeor 公式
        double bmr;
        if (user.getGender() != null && user.getGender() == 1) {
            // 男性
            bmr = 10 * user.getCurrentWeight().doubleValue()
                    + 6.25 * user.getHeight().doubleValue()
                    - 5 * age
                    + 5;
        } else {
            // 女性
            bmr = 10 * user.getCurrentWeight().doubleValue()
                    + 6.25 * user.getHeight().doubleValue()
                    - 5 * age
                    - 161;
        }

        // 活动系数
        double activityFactor = 1.2;
        if (user.getActivityLevel() != null) {
            switch (user.getActivityLevel()) {
                case 2: activityFactor = 1.375; break;
                case 3: activityFactor = 1.55; break;
                case 4: activityFactor = 1.725; break;
                case 5: activityFactor = 1.9; break;
            }
        }

        // 每日总能量消耗
        double tdee = bmr * activityFactor;

        // 根据目标调整
        if (user.getGoalType() != null) {
            if (user.getGoalType() == 1) {
                // 减脂
                tdee = tdee * 0.8;
            } else if (user.getGoalType() == 2) {
                // 增肌
                tdee = tdee * 1.1;
            }
        }

        int result = (int) Math.round(tdee);

        // 最小值保护
        if (result < 800) {
            System.out.println("计算结果异常(" + result + ")，使用默认值2000");
            return 2000;
        }

        // 最大值保护
        if (result > 4000) {
            System.out.println("计算结果异常(" + result + ")，使用默认值2000");
            return 2000;
        }

        return result;
    }
}
package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.User;
import java.util.Map;

public interface UserService extends IService<User> {

    /**
     * 微信登录
     */
    Map<String, Object> loginByWechat(String code, String encryptedData, String iv,
                                      String nickName, String avatarUrl, Boolean silent);

    /**
     * 手机号登录
     */
    Map<String, Object> loginByPhone(String phone, String verifyCode);

    /**
     * 更新用户档案
     */
    void updateProfile(Long userId, User user);

    /**
     * 计算每日热量目标
     */
    Integer calculateDailyCalorieGoal(User user);

    /**
     * 检查账号是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 账号密码注册
     */
    Map<String, Object> register(String username, String password, String phone, String verifyCode, String realCode);

    /**
     * 账号密码登录
     */
    Map<String, Object> loginByPassword(String username, String password);
    /**
     * 邮箱验证码登录
     */
    Map<String, Object> loginByEmail(String email, String verifyCode);

    /**
     * 邮箱+密码注册
     */
    Map<String, Object> registerByEmail(String email, String password, String verifyCode);

    /**
     * 邮箱+密码登录
     */
    Map<String, Object> loginByEmailPassword(String email, String password);

    /**
     * 检查邮箱是否已注册
     */
    boolean isEmailRegistered(String email);

}

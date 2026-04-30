package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.User;
import java.util.Map;

public interface UserService extends IService<User> {

    /**
     * 微信登录（真实实现）
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
}
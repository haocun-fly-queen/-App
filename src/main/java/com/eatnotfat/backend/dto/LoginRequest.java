
package com.eatnotfat.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String code;           // 微信登录code
    private String phone;          // 手机号
    private String verifyCode;     // 验证码
    private String encryptedData;  // 加密的用户信息
    private String iv;             // 加密向量
    private String nickName;       // 昵称（未加密）
    private String avatarUrl;      // 头像URL（未加密）
    private Boolean silent;        // 是否静默登录
}

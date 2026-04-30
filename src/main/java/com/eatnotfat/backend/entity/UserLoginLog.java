package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("eat_not_fat_user_login_log")
public class UserLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户基本信息
    private Long userId;
    private String openid;
    private String phone;
    private String nickname;

    // 登录信息
    private LocalDateTime loginTime;
    private String loginType;
    private Integer loginStatus;
    private String failReason;

    // 网络信息
    private String loginIp;
    private String ipLocation;
    private String networkType;

    // 设备信息
    private String deviceBrand;
    private String deviceModel;
    private String osVersion;
    private String appVersion;
    private String wechatVersion;

    // 运营信息
    private Long referrerId;
    private String channel;
    private String extra;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
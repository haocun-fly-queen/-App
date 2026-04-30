package com.eatnotfat.backend.dto.admin;

import lombok.Data;

/**
 * 登录请求参数 DTO
 *
 * DTO (Data Transfer Object) 数据传输对象
 * 作用：专门用来接收前端传来的参数
 *
 * 为什么要用 DTO？
 * 1. 前端传来的字段名和实体类可能不一致
 * 2. 前端可能只传部分字段
 * 3. 避免暴露实体类的所有字段
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     * 对应前端表单的 username 字段
     */
    private String username;

    /**
     * 密码
     * 对应前端表单的 password 字段
     */
    private String password;
}
package com.eatnotfat.backend.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode("admin123");
        System.out.println("生成的密码: " + encodedPassword);
        System.out.println("密码长度: " + encodedPassword.length());
    }
}
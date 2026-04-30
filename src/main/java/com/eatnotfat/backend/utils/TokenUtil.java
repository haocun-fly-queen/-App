package com.eatnotfat.backend.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 纯 Java 实现 Token 工具类（无任何第三方依赖）
 */
@Component
public class TokenUtil {

    @Value("${token.secret:eat-not-fat-secret-2026}")
    private String secret;

    @Value("${token.expiration:604800000}")
    private Long expiration;

    /**
     * 生成 Token
     */
    public String generateToken(Long userId) {
        long now = System.currentTimeMillis();
        long expireTime = now + expiration;
        long random = (long) (Math.random() * 1000000);

        // 拼接数据: userId|expireTime|random
        String data = userId + "|" + expireTime + "|" + random;

        // Base64 编码
        String encodedData = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));

        // 生成签名 (SHA-256)
        String sign = sha256(data + secret);

        // 返回 Token
        return encodedData + "." + sign;
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return null;
            }

            // 解码数据
            String decodedData = new String(Base64.getDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] dataParts = decodedData.split("\\|");
            if (dataParts.length < 2) {
                return null;
            }

            Long userId = Long.parseLong(dataParts[0]);
            long expireTime = Long.parseLong(dataParts[1]);

            // 检查是否过期
            if (System.currentTimeMillis() > expireTime) {
                System.out.println("Token已过期");
                return null;
            }

            // 验证签名
            String expectedSign = sha256(decodedData + secret);
            if (!expectedSign.equals(parts[1])) {
                System.out.println("Token签名无效");
                return null;
            }

            return userId;
        } catch (Exception e) {
            System.out.println("Token解析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        return getUserIdFromToken(token) != null;
    }

    /**
     * SHA-256 加密
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
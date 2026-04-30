package com.eatnotfat.backend.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * JWT Token 工具类
 *
 * 作用：生成和验证 Token
 * 原理：把用户ID和过期时间编码成字符串，加上签名防止篡改
 *
 * Token 格式：Base64(用户ID|过期时间|角色).签名
 */
@Component
public class JwtUtil {

    // 从配置文件读取密钥（默认值作为备用）
    @Value("${jwt.secret:eat-not-fat-secret-key-2026}")
    private String secret;

    // Token 过期时间（毫秒），默认7天
    @Value("${jwt.expiration:604800000}")
    private Long expiration;

    /**
     * 生成 Token（C端用，只有用户ID）
     * @param userId 用户ID
     * @return Token字符串
     */
    public String generateToken(Long userId) {
        return generateToken(userId, "user");
    }

    /**
     * 生成 Token（B端用，有用户ID和角色）
     * @param userId 用户ID
     * @param role 角色（user/admin/operator）
     * @return Token字符串
     */
    public String generateToken(Long userId, String role) {
        long now = System.currentTimeMillis();
        long expireTime = now + expiration;
        long random = (long) (Math.random() * 1000000);

        // 拼接数据: userId|expireTime|role|random
        String data = userId + "|" + expireTime + "|" + role + "|" + random;

        // Base64 编码
        String encodedData = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));

        // 生成签名 (SHA-256)
        String sign = sha256(data + secret);

        // 返回 Token
        return encodedData + "." + sign;
    }

    /**
     * 从 Token 中获取用户ID
     * @param token Token字符串
     * @return 用户ID
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
     * 从 Token 中获取角色
     * @param token Token字符串
     * @return 角色（user/admin/operator）
     */
    public String getRoleFromToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return null;
            }

            String decodedData = new String(Base64.getDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] dataParts = decodedData.split("\\|");

            if (dataParts.length >= 3) {
                return dataParts[2];  // 第三部分是角色
            }
            return "user";  // 默认角色
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     * @param token Token字符串
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        return getUserIdFromToken(token) != null;
    }

    /**
     * SHA-256 加密
     * @param input 输入字符串
     * @return 加密后的十六进制字符串
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
            // 降级方案
            return Integer.toHexString(input.hashCode());
        }
    }
}
package com.eatnotfat.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private static final Map<String, String> EMAIL_CODE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> EMAIL_CODE_TIME = new ConcurrentHashMap<>();
    private static final long CODE_EXPIRE_MS = 5 * 60 * 1000;

    // 同事Python接口配置
    private static final String PYTHON_API_URL = "http://192.168.1.44:8080/api/send";
    private static final String PYTHON_HOST = "192.168.1.44";
    private static final int PYTHON_PORT = 8080;
    private static final RestTemplate restTemplate = new RestTemplate();

    public void sendVerifyCode(String toEmail) {
        String code = String.format("%06d", new Random().nextInt(999999));
        EMAIL_CODE_CACHE.put(toEmail, code);
        EMAIL_CODE_TIME.put(toEmail, System.currentTimeMillis());

        // 你原来的邮件发送
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("15301779270@163.com");
        message.setTo(toEmail);
        message.setSubject("【鸡仔健康】邮箱验证码");
        message.setText("您的验证码是：" + code + "，5分钟内有效。如非本人操作，请忽略。");
        mailSender.send(message);

        // ========== 调用同事Python接口 + 详细日志 ==========
        callPythonEmailApiWithLog(toEmail, code);

        System.out.println("📧 自己邮箱发送完成 | 邮箱：" + toEmail + " | 验证码：" + code);
    }

    // 带详细日志的调用方法
    private void callPythonEmailApiWithLog(String email, String code) {
        try {
            System.out.println("==============================================");
            System.out.println("🔌 开始调用同事Python邮件接口");
            System.out.println("🌐 接口地址：" + PYTHON_API_URL);
            System.out.println("📍 目标IP：" + PYTHON_HOST);
            System.out.println("🚪 目标端口：" + PYTHON_PORT);
            System.out.println("📩 接收邮箱：" + email);
            System.out.println("🔢 验证码：" + code);
            System.out.println("⏳ 正在发送请求...");

            Map<String, Object> param = new HashMap<>();
            param.put("email", email);
            param.put("code", code);

            restTemplate.postForObject(PYTHON_API_URL, param, String.class);

            System.out.println("✅ Python接口调用【成功】");
            System.out.println("==============================================\n");
        } catch (Exception e) {
            System.err.println("❌ Python接口调用【失败】");
            System.err.println("❌ 失败原因：" + e.getMessage());
            System.err.println("==============================================\n");
        }
    }

    public boolean verifyCode(String email, String code) {
        String realCode = EMAIL_CODE_CACHE.get(email);
        Long sendTime = EMAIL_CODE_TIME.get(email);
        if (realCode == null || sendTime == null) return false;
        if (System.currentTimeMillis() - sendTime > CODE_EXPIRE_MS) {
            EMAIL_CODE_CACHE.remove(email);
            EMAIL_CODE_TIME.remove(email);
            return false;
        }
        boolean match = realCode.equals(code);
        if (match) {
            EMAIL_CODE_CACHE.remove(email);
            EMAIL_CODE_TIME.remove(email);
        }
        return match;
    }
}
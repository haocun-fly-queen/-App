package com.eatnotfat.backend.tools;

import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

public class PythonEmailSender {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String URL = "http://192.168.1.44:8080/api/send";

    public static void send(String email, String code) {
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("email", email);
            param.put("code", code);
            restTemplate.postForObject(URL, param, String.class);
            System.out.println("✅ 已调用同事Python发邮件接口");
        } catch (Exception e) {
            System.err.println("⚠️ Python接口调用失败，但不影响主功能");
        }
    }
}
package com.eatnotfat.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QwenConfig {

    @Value("${qwen.api-key:}")
    private String apiKey;

    // 使用通义千问原生多模态接口
    @Value("${qwen.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}")
    private String apiUrl;

    @Value("${qwen.model:qwen-vl-plus}")
    private String model;

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModel() {
        return model;
    }
}
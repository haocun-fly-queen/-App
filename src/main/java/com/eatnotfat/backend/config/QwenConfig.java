package com.eatnotfat.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "qwen")
public class QwenConfig {

    @Value("${qwen.api-key:}")
    private String apiKey;

    /** 文本模型端点（Qwen-Turbo等，用于饮食/运动/体检评语生成） */
    @Value("${qwen.endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String endpoint;

    /** 多模态模型端点（Qwen-VL，用于图片识别） */
    @Value("${qwen.vl-endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}")
    private String vlEndpoint;

    /** 文本模型名称 */
    @Value("${qwen.model:qwen-turbo}")
    private String model;

    /** 多模态模型名称 */
    @Value("${qwen.vl-model:qwen-vl-plus}")
    private String vlModel;

    /** 最大token数 */
    @Value("${qwen.max-tokens:2000}")
    private Integer maxTokens;

    /** 温度参数 */
    @Value("${qwen.temperature:0.7}")
    private Double temperature;
}

package com.eatnotfat.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    // 当前环境：dev 或 prod
    @Value("${app.env:dev}")
    private String env;

    // OSS配置
    @Value("${oss.endpoint:}")
    private String ossEndpoint;

    @Value("${oss.access-key-id:}")
    private String ossAccessKeyId;

    @Value("${oss.access-key-secret:}")
    private String ossAccessKeySecret;

    @Value("${oss.bucket-name:}")
    private String ossBucketName;

    // 本地存储路径
    @Value("${local.upload-path:./uploads/}")
    private String localUploadPath;

    // 本地访问URL前缀
    @Value("${local.base-url:http://localhost:8080}")
    private String localBaseUrl;

    public String getEnv() {
        return env;
    }

    public boolean isProd() {
        return "prod".equals(env);
    }

    public boolean isDev() {
        return "dev".equals(env);
    }

    public String getOssEndpoint() {
        return ossEndpoint;
    }

    public String getOssAccessKeyId() {
        return ossAccessKeyId;
    }

    public String getOssAccessKeySecret() {
        return ossAccessKeySecret;
    }

    public String getOssBucketName() {
        return ossBucketName;
    }

    public String getLocalUploadPath() {
        return localUploadPath;
    }

    public String getLocalBaseUrl() {
        return localBaseUrl;
    }
}
package com.eatnotfat.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenConfig {

    @Value("${token.secret:eat-not-fat-secret-2026}")
    private String secret;

    @Value("${token.expiration:604800000}")
    private Long expiration;

    public String getSecret() {
        return secret;
    }


    public Long getExpiration() {
        return expiration;
    }
}
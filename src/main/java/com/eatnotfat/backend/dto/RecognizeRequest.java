package com.eatnotfat.backend.dto;

import lombok.Data;

@Data
public class RecognizeRequest {
    private String imageUrl;
        // 图片URL（可选）
        private Long userId;  // 新增：用户ID
    private String imageBase64;   // 图片Base64（新增）
}
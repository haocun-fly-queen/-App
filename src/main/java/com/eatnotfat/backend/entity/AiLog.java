package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("eat_not_fat_ai_log")
public class AiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String imageUrl;

    private String aiProvider;

    private String requestId;

    private String rawResult;

    private String matchedFoodIds;

    private Integer isCorrected;

    private Integer processTimeMs;
    private String aiType; // "recognize" 或 "diet-plan"
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
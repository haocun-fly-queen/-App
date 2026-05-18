package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_indicator")
public class HealthIndicator {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private Long userId;

    private String indicatorName;

    private String indicatorCode;

    private String category;

    private String value;

    private String unit;

    private BigDecimal referenceMin;

    private BigDecimal referenceMax;

    private Integer status;

    private String aiComment;

    private String aiDetail;

    private String aiSuggestion;

    private String foodRelate;

    private LocalDateTime createTime;
}

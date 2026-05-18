package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_report")
public class HealthReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String reportName;

    private LocalDate reportDate;

    private String reportType;

    private String hospital;

    private String fileType;

    private String fileUrl;

    private String rawText;

    private Integer overallScore;

    private String summary;

    private String topConcerns;

    private String positivePoints;

    private String categoryScores;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

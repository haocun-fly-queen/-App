package com.eatnotfat.backend.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HealthReportVO {

    private Long id;

    private String reportName;

    private LocalDate reportDate;

    private String reportType;

    private String hospital;

    private Integer overallScore;

    private Integer abnormalCount;

    private Integer totalCount;

    private Integer status;

    private LocalDateTime createTime;
}

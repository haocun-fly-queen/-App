package com.eatnotfat.backend.dto;

import lombok.Data;

@Data
public class HealthReportUploadDTO {

    private Long userId;

    private String reportName;

    private String reportDate;

    private String reportType;

    private String hospital;
}

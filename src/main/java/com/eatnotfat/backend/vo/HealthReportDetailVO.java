package com.eatnotfat.backend.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class HealthReportDetailVO {

    private Long id;

    private String reportName;

    private LocalDate reportDate;

    private String reportType;

    private String hospital;

    private String fileUrl;

    private Integer overallScore;

    private Integer status;

    private String summary;

    private List<String> topConcerns;

    private List<String> positivePoints;

    private Map<String, Integer> categoryScores;

    private List<IndicatorItemVO> indicators;

    private String disclaimer;

    @Data
    public static class IndicatorItemVO {

        private Long id;

        private String indicatorName;

        private String indicatorCode;

        private String category;

        private String categoryLabel;

        private String value;

        private String unit;

        private String referenceRange;

        private Integer status;

        private String statusLabel;

        private String statusColor;

        private String aiComment;

        private String aiDetail;

        private String aiSuggestion;

        private Map<String, List<String>> foodRelate;
    }
}

package com.eatnotfat.backend.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class HealthScoreVO {

    private Integer overallScore;

    private LocalDate reportDate;

    private Map<String, CategoryScore> categoryScores;

    private String summary;

    private List<String> topConcerns;

    private List<String> positivePoints;

    @Data
    public static class CategoryScore {

        private Integer score;

        private String label;
    }
}

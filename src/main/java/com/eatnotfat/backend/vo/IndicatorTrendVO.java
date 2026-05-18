package com.eatnotfat.backend.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class IndicatorTrendVO {

    private String indicatorName;

    private String unit;

    private BigDecimal referenceMin;

    private BigDecimal referenceMax;

    private List<TrendPoint> trend;

    private String analysis;

    @Data
    public static class TrendPoint {

        private LocalDate reportDate;

        private String value;

        private Integer status;
    }
}

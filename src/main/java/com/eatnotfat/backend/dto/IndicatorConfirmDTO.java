package com.eatnotfat.backend.dto;

import lombok.Data;

@Data
public class IndicatorConfirmDTO {

    private Long indicatorId;

    private String action;

    private String value;

    private String unit;
}

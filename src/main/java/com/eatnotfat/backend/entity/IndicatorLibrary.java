package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("indicator_library")
public class IndicatorLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String aliases;

    private String category;

    private String unit;

    private BigDecimal normalMinMale;

    private BigDecimal normalMaxMale;

    private BigDecimal normalMinFemale;

    private BigDecimal normalMaxFemale;

    private Integer ageAdjusted;

    private String description;

    private String highRiskDesc;

    private String lowRiskDesc;

    private String foodBenefit;

    private String foodAvoid;

    private Integer isEnabled;

    private LocalDateTime createTime;
}

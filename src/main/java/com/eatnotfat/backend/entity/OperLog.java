package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("eatnotfat_sys_oper_log")
public class OperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    private String module;

    private String operation;

    private String method;

    private String params;

    private String result;

    private String ip;

    private Integer costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
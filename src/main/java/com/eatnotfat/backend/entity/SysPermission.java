package com.eatnotfat.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("eat_not_fat_sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String name;
    private String permission;
    private Integer type;
    private String path;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    @TableField(exist = false)
    private List<SysPermission> children;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
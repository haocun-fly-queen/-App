package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper
 * 基础 CRUD 方法已由 BaseMapper 提供，无需额外代码
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
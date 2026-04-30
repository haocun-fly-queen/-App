package com.eatnotfat.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatnotfat.backend.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员 Mapper
 * BaseMapper 提供了基础的 CRUD 方法（增删改查）
 * 我们只需要写自定义的查询方法
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

    /**
     * 根据用户名查询管理员
     * @Select 注解直接写 SQL
     * #{username} 是参数占位符，会自动替换
     */
    @Select("SELECT * FROM eatnotfat_sys_admin WHERE username = #{username}")
    Admin selectByUsername(String username);
}
package com.eatnotfat.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eatnotfat.backend.entity.Role;
import com.eatnotfat.backend.mapper.RoleMapper;
import com.eatnotfat.backend.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色 Service 实现类
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    /**
     * 根据角色代码获取角色
     * LambdaQueryWrapper 是 MyBatis-Plus 提供的条件构造器
     * 用于构建 WHERE 条件，避免写 SQL
     */
    @Override
    public Role getByCode(String roleCode) {
        // 创建条件构造器
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        // 添加条件：role_code = 参数值
        wrapper.eq(Role::getRoleCode, roleCode);
        // 执行查询
        return this.getOne(wrapper);
    }
}
package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.Role;

/**
 * 角色 Service 接口
 */
public interface RoleService extends IService<Role> {

    /**
     * 根据角色代码获取角色
     * @param roleCode 角色代码（如：super_admin, operator）
     * @return 角色对象
     */
    Role getByCode(String roleCode);
}


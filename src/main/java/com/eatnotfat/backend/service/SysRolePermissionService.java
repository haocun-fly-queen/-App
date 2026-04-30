package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.SysRolePermission;

import java.util.List;

public interface SysRolePermissionService extends IService<SysRolePermission> {

    /**
     * 根据角色ID获取权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 分配权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);
}
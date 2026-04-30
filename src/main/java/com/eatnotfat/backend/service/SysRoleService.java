package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eatnotfat.backend.entity.SysRole;

public interface SysRoleService extends IService<SysRole> {

    /**
     * 获取角色编码是否存在
     */
    boolean isRoleCodeExist(String roleCode, Long excludeId);
}
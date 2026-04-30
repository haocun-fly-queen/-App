package com.eatnotfat.backend.controller.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatnotfat.backend.entity.SysPermission;
import com.eatnotfat.backend.entity.SysRolePermission;
import com.eatnotfat.backend.mapper.SysPermissionMapper;
import com.eatnotfat.backend.service.SysRolePermissionService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/permission")
public class SysPermissionController {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private SysRolePermissionService sysRolePermissionService;

    /**
     * 获取权限树（所有菜单和按钮）
     */
    @GetMapping("/tree")
    public Result<List<SysPermission>> getPermissionTree() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysPermission::getSortOrder);
        List<SysPermission> allPermissions = sysPermissionMapper.selectList(wrapper);

        // 构建树形结构
        List<SysPermission> tree = buildTree(allPermissions, 0L);
        return Result.success(tree);
    }

    /**
     * 获取角色已分配的权限ID列表
     */
    @GetMapping("/role/{roleId}")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        List<Long> permissionIds = sysRolePermissionService.getPermissionIdsByRoleId(roleId);
        return Result.success(permissionIds);
    }

    /**
     * 分配权限给角色
     */
    @PutMapping("/role/{roleId}")
    public Result<Void> assignPermissions(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        List<Long> permissionIds = body.get("permissionIds");
        sysRolePermissionService.assignPermissions(roleId, permissionIds);
        return Result.success(null);
    }

    /**
     * 构建权限树
     */
    private List<SysPermission> buildTree(List<SysPermission> all, Long parentId) {
        return all.stream()
                .filter(p -> p.getParentId().equals(parentId))
                .map(p -> {
                    p.setChildren(buildTree(all, p.getId()));
                    return p;
                })
                .collect(Collectors.toList());
    }
}
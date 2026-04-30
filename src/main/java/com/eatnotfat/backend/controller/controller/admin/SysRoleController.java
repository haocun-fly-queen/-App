package com.eatnotfat.backend.controller.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatnotfat.backend.entity.SysRole;
import com.eatnotfat.backend.service.SysRoleService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 获取角色列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getRoleList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysRole::getRoleName, keyword)
                    .or()
                    .like(SysRole::getRoleCode, keyword);
        }
        wrapper.orderByAsc(SysRole::getId);

        Page<SysRole> pageResult = sysRoleService.page(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 获取所有角色（用于下拉框）
     */
    @GetMapping("/all")
    public Result<List<SysRole>> getAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);
        wrapper.orderByAsc(SysRole::getId);
        return Result.success(sysRoleService.list(wrapper));
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public Result<SysRole> getRoleDetail(@PathVariable Long id) {
        SysRole role = sysRoleService.getById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }
        return Result.success(role);
    }

    /**
     * 新增角色
     */
    @PostMapping
    public Result<Void> addRole(@RequestBody SysRole role) {
        // 检查角色编码是否已存在
        if (sysRoleService.isRoleCodeExist(role.getRoleCode(), null)) {
            return Result.error("角色编码已存在");
        }
        role.setStatus(1);
        sysRoleService.save(role);
        return Result.success(null);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        SysRole existing = sysRoleService.getById(id);
        if (existing == null) {
            return Result.error("角色不存在");
        }
        // 检查角色编码是否已存在（排除自身）
        if (sysRoleService.isRoleCodeExist(role.getRoleCode(), id)) {
            return Result.error("角色编码已存在");
        }
        role.setId(id);
        sysRoleService.updateById(role);
        return Result.success(null);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        // 检查是否为内置角色（admin、operator、viewer）
        SysRole role = sysRoleService.getById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }
        String roleCode = role.getRoleCode();
        if ("admin".equals(roleCode) || "operator".equals(roleCode) || "viewer".equals(roleCode)) {
            return Result.error("内置角色不能删除");
        }
        sysRoleService.removeById(id);
        return Result.success(null);
    }

    /**
     * 更新角色状态（启用/禁用）
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateRoleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SysRole role = sysRoleService.getById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }
        Integer status = body.get("status");
        role.setStatus(status);
        sysRoleService.updateById(role);
        return Result.success(null);
    }
}
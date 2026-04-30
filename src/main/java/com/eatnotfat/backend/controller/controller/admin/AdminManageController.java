package com.eatnotfat.backend.controller.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatnotfat.backend.entity.Admin;
import com.eatnotfat.backend.entity.SysRole;
import com.eatnotfat.backend.service.AdminService;
import com.eatnotfat.backend.service.SysRoleService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/manage")
public class AdminManageController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SysRoleService sysRoleService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取管理员列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getAdminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Admin::getUsername, keyword)
                    .or()
                    .like(Admin::getNickname, keyword);
        }
        wrapper.orderByDesc(Admin::getId);

        Page<Admin> pageResult = adminService.page(new Page<>(page, size), wrapper);

        // 填充角色名称
        for (Admin admin : pageResult.getRecords()) {
            if (admin.getRoleId() != null) {
                SysRole role = sysRoleService.getById(admin.getRoleId());
                if (role != null) {
                    admin.setRoleName(role.getRoleName());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 获取管理员详情
     */
    @GetMapping("/{id}")
    public Result<Admin> getAdminDetail(@PathVariable Long id) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        admin.setPassword(null); // 不返回密码
        return Result.success(admin);
    }

    /**
     * 新增管理员
     */
    @PostMapping
    public Result<Void> addAdmin(@RequestBody Admin admin) {
        // 检查用户名是否已存在
        if (adminService.isUsernameExist(admin.getUsername(), null)) {
            return Result.error("用户名已存在");
        }
        // 加密密码
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setStatus(1);
        adminService.save(admin);
        return Result.success(null);
    }

    /**
     * 更新管理员
     */
    @PutMapping("/{id}")
    public Result<Void> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        Admin existing = adminService.getById(id);
        if (existing == null) {
            return Result.error("管理员不存在");
        }
        // 检查用户名是否已存在（排除自身）
        if (adminService.isUsernameExist(admin.getUsername(), id)) {
            return Result.error("用户名已存在");
        }
        admin.setId(id);
        // 如果密码有修改，重新加密
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            admin.setPassword(null); // 不修改密码
        }
        adminService.updateById(admin);
        return Result.success(null);
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        adminService.removeById(id);
        return Result.success(null);
    }

    /**
     * 更新管理员状态（启用/禁用）
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateAdminStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        Integer status = body.get("status");
        admin.setStatus(status);
        adminService.updateById(admin);
        return Result.success(null);
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("新密码不能为空");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        adminService.updateById(admin);
        return Result.success(null);
    }
}
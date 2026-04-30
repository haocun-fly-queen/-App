package com.eatnotfat.backend.controller.controller.admin;

import com.eatnotfat.backend.dto.admin.LoginRequest;
import com.eatnotfat.backend.entity.Admin;
import com.eatnotfat.backend.entity.SysRole;
import com.eatnotfat.backend.service.AdminService;
import com.eatnotfat.backend.service.SysRoleService;
import com.eatnotfat.backend.utils.JwtUtil;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员认证 Controller
 *
 * 作用：处理管理员的登录、登出、获取信息等请求
 * 扮演角色：接待员 - 接收前端请求，调用 Service 处理，返回结果
 *
 * 访问路径前缀：/admin
 */
@RestController
@RequestMapping("/admin")
public class AuthController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // 1. 调用 Service 层验证登录
        Admin admin = adminService.login(request.getUsername(), request.getPassword());

        // 2. 用户名或密码错误
        if (admin == null) {
            return Result.error("用户名或密码错误");
        }

        // 3. 账号被禁用
        if (admin.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }

        // 4. 获取用户角色编码
        String roleCode = "viewer"; // 默认观察员
        if (admin.getRoleId() != null) {
            SysRole role = sysRoleService.getById(admin.getRoleId());
            if (role != null) {
                roleCode = role.getRoleCode();
            }
        }

        // 5. 生成 JWT Token（存储角色信息）
        String token = jwtUtil.generateToken(admin.getId(), roleCode);

        // 6. 获取客户端 IP 地址
        String ip = httpRequest.getRemoteAddr();

        // 7. 更新最后登录信息
        adminService.updateLoginInfo(admin.getId(), ip);

        // 8. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        result.put("roleCode", roleCode);

        return Result.success(result);
            }



    /**
     * 获取当前登录的管理员信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo(@RequestAttribute Long userId) {
        Admin admin = adminService.getById(userId);
        if (admin == null) {
            return Result.error("用户不存在");
        }

        // 获取角色编码
        String roleCode = "viewer";
        if (admin.getRoleId() != null) {
            SysRole role = sysRoleService.getById(admin.getRoleId());
            if (role != null) {
                roleCode = role.getRoleCode();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", admin.getId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        result.put("avatar", admin.getAvatar());
        result.put("roleCode", roleCode);

        return Result.success(result);
    }

    /**
     * 管理员登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success(null);
    }
}
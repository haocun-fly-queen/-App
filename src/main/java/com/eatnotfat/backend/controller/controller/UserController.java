package com.eatnotfat.backend.controller.controller;

import com.eatnotfat.backend.dto.LoginRequest;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.service.UserLoginLogService;
import com.eatnotfat.backend.service.UserService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserLoginLogService userLoginLogService;

    // ===================== 内存存储验证码（不用Redis） =====================
    private static final Map<String, String> CODE_CACHE = new HashMap<>();

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("后端正常运行");
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public Result<Map<String, Object>> sendVerifyCode(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");

        if (phone == null || phone.length() != 11) {
            return Result.error("手机号格式不正确");
        }

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 存入内存
        CODE_CACHE.put(phone, code);

        // 控制台输出（测试用）
        System.out.println("📱 手机号：" + phone + " | 验证码：" + code);

        Map<String, Object> map = new HashMap<>();
        map.put("msg", "验证码发送成功");
        return Result.success(map);
    }

    /**
     * 微信登录
     */
    @PostMapping("/login/wechat")
    public Result<Map<String, Object>> wechatLogin(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Map<String, Object> result = userService.loginByWechat(
                    request.getCode(),
                    request.getEncryptedData(),
                    request.getIv(),
                    request.getNickName(),
                    request.getAvatarUrl(),
                    request.getSilent()
            );

            // 登录成功，记录日志（传入昵称）
            if (result != null && result.get("id") != null) {
                Long userId = ((Number) result.get("id")).longValue();
                User user = userService.getById(userId);
                if (user != null) {
                    userLoginLogService.recordLoginSuccess(user, httpRequest, "wechat", request.getNickName());
                }
            }

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            // 登录失败，记录日志
            userLoginLogService.recordLoginFailure(request.getPhone(), e.getMessage(), httpRequest, "wechat");
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手机号登录
     */
    @PostMapping("/login/phone")
    public Result<Map<String, Object>> phoneLogin(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String phone = request.getPhone();
            String verifyCode = request.getVerifyCode();

            // 校验验证码
            String realCode = CODE_CACHE.get(phone);
            if (realCode == null || !realCode.equals(verifyCode)) {
                // 验证码错误，记录失败日志
                userLoginLogService.recordLoginFailure(phone, "验证码错误或已过期", httpRequest, "phone");
                return Result.error("验证码错误或已过期");
            }

            // 验证通过，删除验证码
            CODE_CACHE.remove(phone);

            // 执行登录
            Map<String, Object> result = userService.loginByPhone(phone, verifyCode);

            // 登录成功，记录日志（手机号登录没有昵称，传 null）
            if (result != null && result.get("id") != null) {
                Long userId = ((Number) result.get("id")).longValue();
                User user = userService.getById(userId);
                if (user != null) {
                    userLoginLogService.recordLoginSuccess(user, httpRequest, "phone", null);
                }
            }

            return Result.success(result);
        } catch (Exception e) {
            // 登录失败，记录日志
            userLoginLogService.recordLoginFailure(request.getPhone(), e.getMessage(), httpRequest, "phone");
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public Result<User> getUser(@PathVariable Long userId) {
        User user = userService.getById(userId);
        return user != null ? Result.success(user) : Result.error("用户不存在");
    }

    /**
     * 更新用户档案
     */
    @PutMapping("/{userId}/profile")
    public Result<Void> updateProfile(@PathVariable Long userId, @RequestBody User user) {
        try {
            userService.updateProfile(userId, user);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 计算每日热量目标
     */
    @GetMapping("/{userId}/calorie-goal")
    public Result<Integer> getCalorieGoal(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        Integer goal = userService.calculateDailyCalorieGoal(user);
        return Result.success(goal);
    }
}
package com.eatnotfat.backend.controller;

import com.eatnotfat.backend.dto.LoginRequest;
import com.eatnotfat.backend.dto.RegisterDTO;
import com.eatnotfat.backend.dto.PasswordLoginDTO;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.service.EmailService;
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

    @Autowired
    private EmailService emailService;

    // ===================== 内存存储验证码 =====================
    private static final Map<String, String> CODE_CACHE = new HashMap<>();

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("后端正常运行");
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/send-code")
    public Result<Map<String, Object>> sendVerifyCode(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");

        if (phone == null || phone.length() != 11) {
            return Result.error("手机号格式不正确");
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        CODE_CACHE.put(phone, code);
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

            String realCode = CODE_CACHE.get(phone);
            if (realCode == null || !realCode.equals(verifyCode)) {
                userLoginLogService.recordLoginFailure(phone, "验证码错误或已过期", httpRequest, "phone");
                return Result.error("验证码错误或已过期");
            }

            CODE_CACHE.remove(phone);

            Map<String, Object> result = userService.loginByPhone(phone, verifyCode);

            if (result != null && result.get("id") != null) {
                Long userId = ((Number) result.get("id")).longValue();
                User user = userService.getById(userId);
                if (user != null) {
                    userLoginLogService.recordLoginSuccess(user, httpRequest, "phone", null);
                }
            }

            return Result.success(result);
        } catch (Exception e) {
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

    /**
     * 检查账号是否可用
     */
    @GetMapping("/check-username")
    public Result<Map<String, Object>> checkUsername(@RequestParam String username) {
        if (username == null || username.length() < 3) {
            return Result.error("账号至少3个字符");
        }
        boolean available = userService.isUsernameAvailable(username);
        Map<String, Object> data = new HashMap<>();
        data.put("available", available);
        return Result.success(data);
    }

    /**
     * 账号密码注册
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterDTO dto) {
        try {
            if (dto.getUsername() == null || dto.getUsername().length() < 3) {
                return Result.error("账号至少3个字符");
            }
            if (dto.getPassword() == null || dto.getPassword().length() < 6) {
                return Result.error("密码至少6位");
            }
            if (dto.getPhone() == null || dto.getPhone().length() != 11) {
                return Result.error("手机号格式不正确");
            }
            if (dto.getVerifyCode() == null) {
                return Result.error("请输入验证码");
            }

            String realCode = CODE_CACHE.get(dto.getPhone());

            Map<String, Object> result = userService.register(
                    dto.getUsername(), dto.getPassword(), dto.getPhone(),
                    dto.getVerifyCode(), realCode
            );

            CODE_CACHE.remove(dto.getPhone());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 账号密码登录
     */
    @PostMapping("/login-password")
    public Result<Map<String, Object>> loginByPassword(@RequestBody PasswordLoginDTO dto) {
        try {
            if (dto.getUsername() == null || dto.getPassword() == null) {
                return Result.error("请输入账号和密码");
            }
            Map<String, Object> result = userService.loginByPassword(dto.getUsername(), dto.getPassword());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ===================== 邮箱相关接口 =====================

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-email-code")
    public Result<Map<String, Object>> sendEmailCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        if (email == null || !email.contains("@")) {
            return Result.error("邮箱格式不正确");
        }
        try {
            emailService.sendVerifyCode(email);
            Map<String, Object> data = new HashMap<>();
            data.put("msg", "验证码已发送至 " + email);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 邮箱验证码登录
     */
    @PostMapping("/login-email")
    public Result<Map<String, Object>> loginByEmail(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String code = params.get("verifyCode");
            if (email == null || code == null) {
                return Result.error("请输入邮箱和验证码");
            }
            Map<String, Object> result = userService.loginByEmail(email, code);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 邮箱+密码注册
     */
    @PostMapping("/register-email")
    public Result<Map<String, Object>> registerByEmail(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");
            String code = params.get("verifyCode");
            if (email == null || password == null || code == null) {
                return Result.error("请填写完整信息");
            }
            if (password.length() < 6) {
                return Result.error("密码至少6位");
            }

            // 你原来的注册逻辑（不动）
            Map<String, Object> result = userService.registerByEmail(email, password, code);

            // ===================== 注册成功 → 把邮箱传给同事 =====================
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                java.util.Map<String, Object> sendParams = new java.util.HashMap<>();
                sendParams.put("email", email); // 只传注册邮箱

                // 调用同事接口
                restTemplate.postForObject("http://192.168.1.44:8080/api/send", sendParams, String.class);
                System.out.println("✅ 注册成功，已把邮箱推送给同事接口：" + email);
            } catch (Exception e) {
                System.err.println("⚠️ 推送邮箱接口调用失败：" + e.getMessage());
            }
            // ====================================================================

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 邮箱+密码登录
     */
    @PostMapping("/login-email-password")
    public Result<Map<String, Object>> loginByEmailPassword(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String password = params.get("password");
            if (email == null || password == null) {
                return Result.error("请输入邮箱和密码");
            }
            Map<String, Object> result = userService.loginByEmailPassword(email, password);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查邮箱是否已注册
     */
    @GetMapping("/check-email")
    public Result<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean registered = userService.isEmailRegistered(email);
        Map<String, Object> data = new HashMap<>();
        data.put("registered", registered);
        return Result.success(data);
    }
}

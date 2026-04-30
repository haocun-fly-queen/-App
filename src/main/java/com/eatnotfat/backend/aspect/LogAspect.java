package com.eatnotfat.backend.aspect;

import com.alibaba.fastjson2.JSON;
import com.eatnotfat.backend.entity.SysOperLog;
import com.eatnotfat.backend.service.SysOperLogService;
import com.eatnotfat.backend.utils.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysOperLogService sysOperLogService;

    @Autowired
    private JwtUtil jwtUtil;

    // 定义切点：admin 包下的所有 Controller 方法
    @Pointcut("execution(* com.eatnotfat.backend.controller.admin..*.*(..))")
    public void adminLog() {}

    @Around("adminLog()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        // 获取请求参数 - 过滤掉 HttpServletRequest 和 HttpServletResponse
        Object[] args = joinPoint.getArgs();
        String params = "";
        try {
            Object[] filteredArgs = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof HttpServletRequest))
                    .toArray();
            params = filteredArgs.length > 0 ? JSON.toJSONString(filteredArgs) : "";
        } catch (Exception e) {
            params = "参数序列化失败";
        }

        // 获取当前登录用户信息
        String username = getUsernameFromRequest(request);

        // 执行方法
        Object result = null;
        Exception exception = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 保存日志
            SysOperLog log = new SysOperLog();
            log.setUsername(username);
            log.setModule(className.replace("Controller", ""));
            log.setOperation(methodName);
            log.setRequestUrl(request.getRequestURI());
            log.setRequestMethod(request.getMethod());
            log.setRequestParams(params.length() > 2000 ? params.substring(0, 2000) : params);
            if (result != null) {
                try {
                    String responseStr = JSON.toJSONString(result);
                    log.setResponseResult(responseStr.length() > 2000 ? responseStr.substring(0, 2000) : responseStr);
                } catch (Exception e) {
                    log.setResponseResult("响应序列化失败");
                }
            }
            log.setIpAddress(getIpAddress(request));
            log.setDurationMs((int) duration);
            log.setStatus(exception == null ? 1 : 0);
            if (exception != null) {
                String errorMsg = exception.getMessage();
                log.setErrorMsg(errorMsg != null && errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
            }

            try {
                sysOperLogService.recordLog(log);
            } catch (Exception e) {
                System.err.println("保存操作日志失败: " + e.getMessage());
            }
        }
    }

    /**
     * 从请求中获取当前登录用户名
     */
    private String getUsernameFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return "admin_" + userId;
                }
            }
        } catch (Exception e) {
            System.out.println("获取用户名失败: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * 获取客户端IP
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
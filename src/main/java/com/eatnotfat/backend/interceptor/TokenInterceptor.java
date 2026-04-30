package com.eatnotfat.backend.interceptor;

import com.eatnotfat.backend.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtil tokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 临时关闭拦截器，用于测试
        return true;

        /* 原来的代码暂时注释
        String path = request.getRequestURI();
        if (path.contains("/api/user/login") ||
                path.contains("/api/user/test") ||
                path.contains("/api/upload")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\"}");
            return false;
        }

        String token = authHeader.substring(7);

        if (!tokenUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }

        Long userId = tokenUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);

        return true;
        */
    }
}
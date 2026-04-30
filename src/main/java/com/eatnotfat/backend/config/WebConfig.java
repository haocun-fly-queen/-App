package com.eatnotfat.backend.config;

import com.eatnotfat.backend.interceptor.TokenInterceptor;
import com.eatnotfat.backend.interceptor.AdminTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;        // C端拦截器

    @Autowired
    private AdminTokenInterceptor adminTokenInterceptor;  // B端拦截器（新增）

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // ========== C端拦截器（给小程序用的）==========
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")                      // 拦截所有api
                .excludePathPatterns("/api/user/login/**")       // 放行登录
                .excludePathPatterns("/api/user/test")           // 放行测试
                .excludePathPatterns("/api/upload");             // 放行上传

        // ========== B端拦截器（给管理后台用的）==========
        registry.addInterceptor(adminTokenInterceptor)
                .addPathPatterns("/admin/**")                    // 拦截所有/admin开头的请求
                .excludePathPatterns("/admin/login");            // 放行登录接口
    }
}
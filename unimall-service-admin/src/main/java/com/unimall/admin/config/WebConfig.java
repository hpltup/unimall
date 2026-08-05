package com.unimall.admin.config;

import com.unimall.common.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer
{
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor)
    {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Bean
    public JwtUtil jwtUtil(@Value("${unimall.jwt.secret}") String secret,
                           @Value("${unimall.jwt.expire-seconds:1800}") long expireSeconds)
    {
        return new JwtUtil(secret, expireSeconds);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }
}

package com.unimall.admin.config;

import com.unimall.common.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JwtUtil Bean：从配置中心共享配置（application.yml 的 unimall.jwt）构造
 */
@Configuration
public class JwtConfig
{
    @Value("${unimall.jwt.secret}")
    private String secret;

    @Value("${unimall.jwt.expire-seconds:1800}")
    private long expireSeconds;

    @Bean
    public JwtUtil jwtUtil()
    {
        return new JwtUtil(secret, expireSeconds);
    }
}

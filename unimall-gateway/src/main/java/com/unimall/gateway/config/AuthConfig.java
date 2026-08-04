package com.unimall.gateway.config;

import com.unimall.common.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig
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

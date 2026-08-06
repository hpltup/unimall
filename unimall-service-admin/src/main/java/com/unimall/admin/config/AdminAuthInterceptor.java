package com.unimall.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimall.common.result.Result;
import com.unimall.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 管理面鉴权拦截器：校验 Authorization: Bearer <token> + Redis admin:token:{jti} 存在。
 * 校验通过时**续期会话**（滑动超时）。
 * （网关白名单已放行 /api/admin，管理面安全由本拦截器负责）
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor
{
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String REDIS_TOKEN_PREFIX = "admin:token:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    /** 会话滑动超时（秒）：每次校验通过重置 TTL */
    @Value("${unimall.jwt.session-seconds:1800}")
    private long sessionSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminAuthInterceptor(JwtUtil jwtUtil, StringRedisTemplate redisTemplate)
    {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX))
        {
            return unauthorized(response);
        }
        String token = authHeader.substring(TOKEN_PREFIX.length());

        Claims claims;
        try
        {
            claims = jwtUtil.parseToken(token);
        }
        catch (JwtException | IllegalArgumentException e)
        {
            return unauthorized(response);
        }

        String key = REDIS_TOKEN_PREFIX + jwtUtil.getJti(claims);
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists))
        {
            // 滑动续期：重置会话超时
            redisTemplate.expire(key, Duration.ofSeconds(sessionSeconds));
            return true;
        }
        return unauthorized(response);
    }

    private boolean unauthorized(HttpServletResponse response) throws Exception
    {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "未登录或token已失效")));
        return false;
    }
}

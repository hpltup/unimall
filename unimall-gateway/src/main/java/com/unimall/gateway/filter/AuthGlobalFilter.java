package com.unimall.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimall.common.result.Result;
import com.unimall.common.utils.JwtUtil;
import com.unimall.gateway.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局 JWT 鉴权过滤器（Redis 白名单模式）
 * 白名单直接放行；其余请求校验 Authorization: Bearer <token>，
 * 解析成功且 Redis 中存在 login:token:{jti} 才放行，并附加 X-User-Id 头
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered
{
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String REDIS_TOKEN_PREFIX = "login:token:";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthGlobalFilter(JwtUtil jwtUtil, AuthProperties authProperties, ReactiveStringRedisTemplate redisTemplate)
    {
        this.jwtUtil = jwtUtil;
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单放行
        if (authProperties.getWhitelist().stream().anyMatch(path::startsWith))
        {
            return chain.filter(exchange);
        }

        // 2. 取 token
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX))
        {
            return unauthorized(exchange.getResponse());
        }
        String token = authHeader.substring(TOKEN_PREFIX.length());

        // 3. 解析 JWT（验签 + 过期校验）
        Claims claims;
        try
        {
            claims = jwtUtil.parseToken(token);
        }
        catch (JwtException | IllegalArgumentException e)
        {
            return unauthorized(exchange.getResponse());
        }

        String jti = jwtUtil.getJti(claims);
        Long userId = jwtUtil.getUserId(claims);

        // 4. Redis 白名单校验：key 存在才放行（登出/过期已被删除）
        return redisTemplate.opsForValue()
                .get(REDIS_TOKEN_PREFIX + jti)
                .flatMap(value -> {
                    ServerWebExchange mutated = exchange.mutate()
                            .request(builder -> builder.header(USER_ID_HEADER, String.valueOf(userId)))
                            .build();
                    return chain.filter(mutated);
                })
                .switchIfEmpty(unauthorized(exchange.getResponse()));
    }

    private Mono<Void> unauthorized(ServerHttpResponse response)
    {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try
        {
            bytes = objectMapper.writeValueAsBytes(Result.fail(401, "未登录或token已失效"));
        }
        catch (Exception e)
        {
            bytes = "{\"code\":401,\"message\":\"未登录或token已失效\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder()
    {
        return -100;
    }
}

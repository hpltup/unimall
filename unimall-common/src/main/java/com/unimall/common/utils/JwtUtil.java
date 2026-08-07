package com.unimall.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 * 实际签名算法由 jjwt 按密钥长度自动选择：>= 48 字节用 HS512（当前密钥 64 字节 → HS512）
 * 纯 Java，无 Spring 依赖，网关（校验）与 user 服务（签发）共用
 */
public class JwtUtil
{
    private final SecretKey key;
    private final long expireSeconds;

    /**
     * @param secret        对称密钥，必须 >= 32 字节（256 bit），64 字节时为 HS512
     * @param expireSeconds token 有效期（秒）
     */
    public JwtUtil(String secret, long expireSeconds)
    {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    /**
     * 生成 token：sub=userId，jti=UUID（Redis 白名单 key 用），带 iat/exp
     */
    public String generateToken(Long userId)
    {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并验签 token，失败抛 JwtException / IllegalArgumentException
     */
    public Claims parseToken(String token)
    {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getJti(Claims claims)
    {
        return claims.getId();
    }

    public Long getUserId(Claims claims)
    {
        return Long.valueOf(claims.getSubject());
    }

    /**
     * token 有效期（秒），签发端存 Redis 白名单 TTL 时使用，保证与 JWT 过期一致
     */
    public long getExpireSeconds()
    {
        return expireSeconds;
    }
}

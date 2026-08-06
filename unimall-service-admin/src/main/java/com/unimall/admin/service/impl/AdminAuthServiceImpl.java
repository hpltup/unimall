package com.unimall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimall.admin.mapper.IAdminUserMapper;
import com.unimall.admin.pojo.dto.AdminLoginDTO;
import com.unimall.admin.pojo.entity.AdminUser;
import com.unimall.admin.pojo.vo.AdminLoginVO;
import com.unimall.admin.service.IAdminAuthService;
import com.unimall.common.exception.BusinessException;
import com.unimall.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AdminAuthServiceImpl implements IAdminAuthService
{
    private static final String REDIS_TOKEN_PREFIX = "admin:token:";

    private final IAdminUserMapper adminUserMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    /** 会话滑动超时（秒）：30 分钟无操作则 Redis 白名单 key 过期 */
    private final long sessionSeconds;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthServiceImpl(IAdminUserMapper adminUserMapper, StringRedisTemplate redisTemplate, JwtUtil jwtUtil,
                                @Value("${unimall.jwt.session-seconds:1800}") long sessionSeconds)
    {
        this.adminUserMapper = adminUserMapper;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.sessionSeconds = sessionSeconds;
    }

    @Override
    public AdminLoginVO login(AdminLoginDTO dto)
    {
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, dto.getUsername()));
        if (admin == null)
        {
            throw new BusinessException(9001, "管理员不存在");
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword()))
        {
            throw new BusinessException(9002, "密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0)
        {
            throw new BusinessException(9003, "管理员已被禁用");
        }

        // 签发 JWT（有效期 7 天兜底）并写入 Redis 白名单（TTL = 会话滑动超时）
        String token = jwtUtil.generateToken(admin.getId());
        Claims claims = jwtUtil.parseToken(token);
        redisTemplate.opsForValue().set(
                REDIS_TOKEN_PREFIX + jwtUtil.getJti(claims),
                String.valueOf(admin.getId()),
                Duration.ofSeconds(sessionSeconds));

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setAdminId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        return vo;
    }

    @Override
    public void logout(String token)
    {
        try
        {
            Claims claims = jwtUtil.parseToken(token);
            redisTemplate.delete(REDIS_TOKEN_PREFIX + jwtUtil.getJti(claims));
        }
        catch (Exception ignored)
        {
            // token 已失效则无需处理
        }
    }
}

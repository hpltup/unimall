package com.unimall.admin.service.impl;

import com.unimall.admin.mapper.IAdminUserMapper;
import com.unimall.admin.pojo.dto.AdminLoginDTO;
import com.unimall.admin.pojo.entity.AdminUser;
import com.unimall.admin.pojo.vo.AdminLoginVO;
import com.unimall.admin.service.IAdminAuthService;
import com.unimall.common.exception.BusinessException;
import com.unimall.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
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
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthServiceImpl(IAdminUserMapper adminUserMapper, StringRedisTemplate redisTemplate, JwtUtil jwtUtil)
    {
        this.adminUserMapper = adminUserMapper;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AdminLoginVO login(AdminLoginDTO dto)
    {
        AdminUser admin = adminUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminUser>()
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

        String token = jwtUtil.generateToken(admin.getId());
        Claims claims = jwtUtil.parseToken(token);
        redisTemplate.opsForValue().set(
                REDIS_TOKEN_PREFIX + jwtUtil.getJti(claims),
                String.valueOf(admin.getId()),
                Duration.ofSeconds(jwtUtil.getExpireSeconds()));

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

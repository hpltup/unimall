package com.unimall.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.common.exception.BusinessException;
import com.unimall.common.utils.JwtUtil;
import com.unimall.user.mapper.IUserMapper;
import com.unimall.user.pojo.dto.LoginDTO;
import com.unimall.user.pojo.dto.RegisterDTO;
import com.unimall.user.pojo.entity.User;
import com.unimall.user.pojo.vo.LoginVO;
import com.unimall.common.vo.UserVO;
import com.unimall.user.service.IUserService;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UserServiceImpl extends ServiceImpl<IUserMapper, User> implements IUserService
{
    private static final String REDIS_TOKEN_PREFIX = "login:token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(StringRedisTemplate redisTemplate, JwtUtil jwtUtil)
    {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Long register(RegisterDTO dto)
    {
        Long count = lambdaQuery().eq(User::getUsername, dto.getUsername()).count();
        if (count > 0)
        {
            throw new BusinessException(1001, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null ? dto.getUsername() : dto.getNickname());
        save(user);
        return user.getId();
    }

    @Override
    public LoginVO login(LoginDTO dto)
    {
        User user = lambdaQuery().eq(User::getUsername, dto.getUsername()).one();
        if (user == null)
        {
            throw new BusinessException(1002, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword()))
        {
            throw new BusinessException(1003, "密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0)
        {
            throw new BusinessException(1004, "账号已被禁用");
        }

        // 签发 JWT 并写入 Redis 白名单（TTL 与 JWT 有效期一致）
        String token = jwtUtil.generateToken(user.getId());
        Claims claims = jwtUtil.parseToken(token);
        redisTemplate.opsForValue().set(
                REDIS_TOKEN_PREFIX + jwtUtil.getJti(claims),
                String.valueOf(user.getId()),
                Duration.ofSeconds(jwtUtil.getExpireSeconds()));

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        return vo;
    }

    @Override
    public UserVO info(Long userId)
    {
        User user = getById(userId);
        if (user == null)
        {
            throw new BusinessException(1002, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public Page<UserVO> adminPage(Integer pageNum, Integer pageSize, String keyword)
    {
        Page<User> page = lambdaQuery()
                .like(StringUtils.isNotBlank(keyword), User::getUsername, keyword)
                .or(StringUtils.isNotBlank(keyword), wrapper -> wrapper.like(User::getNickname, keyword))
                .orderByDesc(User::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public void adminUpdateStatus(Long id, Integer status)
    {
        User user = getById(id);
        if (user == null)
        {
            throw new BusinessException(1002, "用户不存在");
        }
        lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getStatus, status)
                .update();
    }

    private UserVO toVO(User user)
    {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setLevel(user.getLevel());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}

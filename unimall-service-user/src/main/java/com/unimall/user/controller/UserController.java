package com.unimall.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.dto.UserStatusDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.UserVO;
import com.unimall.user.pojo.dto.LoginDTO;
import com.unimall.user.pojo.dto.RegisterDTO;
import com.unimall.user.pojo.vo.LoginVO;
import com.unimall.user.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController
{
    private final IUserService userService;

    public UserController(IUserService userService)
    {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid RegisterDTO dto)
    {
        return Result.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto)
    {
        return Result.ok(userService.login(dto));
    }

    @GetMapping("/info")
    public Result<UserVO> info(@RequestHeader("X-User-Id") Long userId)
    {
        return Result.ok(userService.info(userId));
    }

    /**
     * 登出（需登录）：删除 Redis 白名单 key，token 立即失效
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization)
    {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        userService.logout(token);
        return Result.ok();
    }

    /**
     * 服务间内部接口（admin 调用，不走网关）：用户分页
     */
    @GetMapping("/internal/admin-list")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO>> adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword)
    {
        return Result.ok(userService.adminPage(pageNum, pageSize, keyword));
    }

    /**
     * 服务间内部接口（admin 调用）：禁用/启用
     */
    @PutMapping("/internal/admin-status")
    public Result<Void> adminUpdateStatus(@RequestBody @Valid UserStatusDTO dto)
    {
        userService.adminUpdateStatus(dto.getId(), dto.getStatus());
        return Result.ok();
    }
}

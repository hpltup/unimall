package com.unimall.user.controller;

import com.unimall.common.result.Result;
import com.unimall.user.pojo.dto.LoginDTO;
import com.unimall.user.pojo.dto.RegisterDTO;
import com.unimall.user.pojo.vo.LoginVO;
import com.unimall.user.pojo.vo.UserVO;
import com.unimall.user.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
}

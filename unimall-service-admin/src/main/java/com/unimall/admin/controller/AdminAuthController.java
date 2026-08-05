package com.unimall.admin.controller;

import com.unimall.admin.pojo.dto.AdminLoginDTO;
import com.unimall.admin.pojo.vo.AdminLoginVO;
import com.unimall.admin.service.IAdminAuthService;
import com.unimall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminAuthController
{
    private final IAdminAuthService adminAuthService;

    public AdminAuthController(IAdminAuthService adminAuthService)
    {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody @Valid AdminLoginDTO dto)
    {
        return Result.ok(adminAuthService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization)
    {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        adminAuthService.logout(token);
        return Result.ok();
    }
}

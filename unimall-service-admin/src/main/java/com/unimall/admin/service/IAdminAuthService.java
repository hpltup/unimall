package com.unimall.admin.service;

import com.unimall.admin.pojo.dto.AdminLoginDTO;
import com.unimall.admin.pojo.vo.AdminLoginVO;

public interface IAdminAuthService
{
    /**
     * 管理员登录：校验密码 → 签发 JWT + 写 Redis 白名单（admin:token:{jti}）
     */
    AdminLoginVO login(AdminLoginDTO dto);

    /**
     * 登出：删除 Redis 白名单 key
     */
    void logout(String token);
}

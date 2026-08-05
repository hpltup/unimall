package com.unimall.user.service;

import com.unimall.user.pojo.dto.LoginDTO;
import com.unimall.user.pojo.dto.RegisterDTO;
import com.unimall.user.pojo.vo.LoginVO;
import com.unimall.common.vo.UserVO;

public interface IUserService
{
    /**
     * 注册，返回新用户 id
     */
    Long register(RegisterDTO dto);

    /**
     * 登录，签发 JWT 并写入 Redis 白名单，返回 token
     */
    LoginVO login(LoginDTO dto);

    /**
     * 查询用户信息（不返回敏感字段）
     */
    UserVO info(Long userId);

    /**
     * 服务间调用（admin）：用户分页（可按用户名/昵称模糊查询）
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO> adminPage(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 服务间调用（admin）：禁用/启用
     */
    void adminUpdateStatus(Long id, Integer status);
}

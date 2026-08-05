package com.unimall.admin.pojo.vo;

import lombok.Data;

@Data
public class AdminLoginVO
{
    private String token;

    private Long adminId;

    private String username;

    private Long expiresIn;
}

package com.unimall.common.dto;

/**
 * 用户禁用/启用入参（跨服务共享）：admin 调 user 服务
 */
public class UserStatusDTO
{
    private Long id;

    /** 0禁用 1正常 */
    private Integer status;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }
}

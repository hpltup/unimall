package com.unimall.common.dto;

/**
 * 商品上下架入参（跨服务共享）：admin 调 goods 服务
 */
public class GoodsStatusDTO
{
    private Long id;

    /** 0下架 1上架 */
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

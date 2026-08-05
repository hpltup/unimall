package com.unimall.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动创建入参（跨服务共享）：admin 调 seckill 服务
 */
public class SeckillActivityCreateDTO
{
    private Long goodsId;

    private String goodsName;

    private String goodsImage;

    private BigDecimal seckillPrice;

    private Integer stock;

    private Integer limitPerUser;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public Long getGoodsId()
    {
        return goodsId;
    }

    public void setGoodsId(Long goodsId)
    {
        this.goodsId = goodsId;
    }

    public String getGoodsName()
    {
        return goodsName;
    }

    public void setGoodsName(String goodsName)
    {
        this.goodsName = goodsName;
    }

    public String getGoodsImage()
    {
        return goodsImage;
    }

    public void setGoodsImage(String goodsImage)
    {
        this.goodsImage = goodsImage;
    }

    public BigDecimal getSeckillPrice()
    {
        return seckillPrice;
    }

    public void setSeckillPrice(BigDecimal seckillPrice)
    {
        this.seckillPrice = seckillPrice;
    }

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    public Integer getLimitPerUser()
    {
        return limitPerUser;
    }

    public void setLimitPerUser(Integer limitPerUser)
    {
        this.limitPerUser = limitPerUser;
    }

    public LocalDateTime getStartTime()
    {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime)
    {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime()
    {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime)
    {
        this.endTime = endTime;
    }
}

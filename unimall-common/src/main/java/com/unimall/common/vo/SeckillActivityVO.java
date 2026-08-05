package com.unimall.common.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动出参（跨服务共享）：seckill 服务返回，admin 服务 Feign 消费
 */
public class SeckillActivityVO
{
    private Long id;

    private Long goodsId;

    private String goodsName;

    private String goodsImage;

    private BigDecimal seckillPrice;

    private Integer stock;

    private Integer limitPerUser;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 0未开始 1进行中 2已结束（按当前时间计算） */
    private Integer status;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

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

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }
}

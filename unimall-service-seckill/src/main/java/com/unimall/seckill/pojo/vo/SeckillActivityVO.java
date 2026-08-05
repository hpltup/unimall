package com.unimall.seckill.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
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
}

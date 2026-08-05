package com.unimall.seckill.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_order")
public class SeckillOrder
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long activityId;

    private Long userId;

    private Long goodsId;

    private String goodsName;

    private String goodsImage;

    private BigDecimal seckillPrice;

    private Integer quantity;

    private BigDecimal total;

    /** 0待付款 1已付款 2已完成 3已取消 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

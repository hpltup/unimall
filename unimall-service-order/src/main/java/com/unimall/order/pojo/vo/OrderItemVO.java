package com.unimall.order.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO
{
    private Long id;

    private Long goodsId;

    private String goodsName;

    private String goodsImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal total;
}

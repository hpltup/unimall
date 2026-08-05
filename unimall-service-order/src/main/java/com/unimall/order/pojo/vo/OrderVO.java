package com.unimall.order.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO
{
    private Long id;

    private String orderNo;

    private BigDecimal totalAmount;

    /** 0待付款 1已付款 2已完成 3已取消 */
    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private List<OrderItemVO> items;
}

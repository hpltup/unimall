package com.unimall.common.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单出参（跨服务共享）：order 服务返回，admin 服务 Feign 消费
 */
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

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public LocalDateTime getPayTime()
    {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime)
    {
        this.payTime = payTime;
    }

    public LocalDateTime getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime)
    {
        this.createTime = createTime;
    }

    public List<OrderItemVO> getItems()
    {
        return items;
    }

    public void setItems(List<OrderItemVO> items)
    {
        this.items = items;
    }
}

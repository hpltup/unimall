package com.unimall.common.dto;

/**
 * 库存操作入参（跨服务共享）：goods 服务扣/回库存，order/seckill 经 Feign 传入
 */
public class GoodsStockDTO
{
    private Long goodsId;

    private Integer quantity;

    public Long getGoodsId()
    {
        return goodsId;
    }

    public void setGoodsId(Long goodsId)
    {
        this.goodsId = goodsId;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }
}

package com.unimall.common.vo;

import java.math.BigDecimal;

/**
 * 购物车条目出参（跨服务共享）：cart 服务返回，order 服务 Feign 消费
 */
public class CartItemVO
{
    private Long id;

    private Long goodsId;

    private String goodsName;

    private String mainImage;

    private BigDecimal price;

    private Integer quantity;

    private Integer checked;

    /** 小计 = price * quantity */
    private BigDecimal total;

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

    public String getMainImage()
    {
        return mainImage;
    }

    public void setMainImage(String mainImage)
    {
        this.mainImage = mainImage;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public Integer getChecked()
    {
        return checked;
    }

    public void setChecked(Integer checked)
    {
        this.checked = checked;
    }

    public BigDecimal getTotal()
    {
        return total;
    }

    public void setTotal(BigDecimal total)
    {
        this.total = total;
    }
}

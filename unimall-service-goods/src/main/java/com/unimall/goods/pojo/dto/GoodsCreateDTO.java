package com.unimall.goods.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsCreateDTO
{
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subTitle;

    private String mainImage;

    private String images;

    private String detail;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private BigDecimal marketPrice;

    @NotNull(message = "库存不能为空")
    private Integer stock;
}

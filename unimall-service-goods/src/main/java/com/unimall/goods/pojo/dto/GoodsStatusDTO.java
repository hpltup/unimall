package com.unimall.goods.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GoodsStatusDTO
{
    @NotNull(message = "商品ID不能为空")
    private Long id;

    /** 0下架 1上架 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}

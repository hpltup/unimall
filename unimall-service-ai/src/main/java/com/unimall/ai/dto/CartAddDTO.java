package com.unimall.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加购入参（AI 模块本地定义，Feign 调用 cart 服务 /cart/add 用，字段与 cart 侧 CartAddDTO 一致）
 */
@Data
public class CartAddDTO
{
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}

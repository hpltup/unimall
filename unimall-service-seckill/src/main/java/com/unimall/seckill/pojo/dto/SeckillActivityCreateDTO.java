package com.unimall.seckill.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityCreateDTO
{
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    private String goodsImage;

    @NotNull(message = "秒杀价不能为空")
    private BigDecimal seckillPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "库存至少为1")
    private Integer stock;

    @NotNull(message = "限购数量不能为空")
    @Min(value = 1, message = "限购至少为1")
    private Integer limitPerUser;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}

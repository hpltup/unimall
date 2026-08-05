package com.unimall.order.client;

import com.unimall.common.dto.GoodsStockDTO;
import com.unimall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * goods 服务 Feign 客户端（扣/回库存）
 */
@FeignClient(name = "unimall-service-goods")
public interface IGoodsClient
{
    @PostMapping("/goods/deduct")
    Result<Void> deductStock(@RequestBody GoodsStockDTO dto);

    @PostMapping("/goods/restore")
    Result<Void> restoreStock(@RequestBody GoodsStockDTO dto);
}

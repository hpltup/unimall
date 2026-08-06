package com.unimall.seckill.client;

import com.unimall.common.dto.GoodsStockDTO;
import com.unimall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * goods 服务 Feign 客户端（秒杀建单后联动扣减商品库存）
 */
@FeignClient(name = "unimall-service-goods")
public interface IGoodsClient
{
    @PostMapping("/goods/deduct")
    Result<Void> deductStock(@RequestBody GoodsStockDTO dto);
}

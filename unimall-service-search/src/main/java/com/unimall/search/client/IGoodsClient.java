package com.unimall.search.client;

import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * goods 服务 Feign 客户端（全量同步商品数据到 ES）
 */
@FeignClient(name = "unimall-service-goods")
public interface IGoodsClient
{
    @GetMapping("/goods/internal/for-search")
    Result<List<GoodsVO>> allOnSale();
}

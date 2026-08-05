package com.unimall.cart.client;

import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * goods 服务 Feign 客户端（服务间直连，不经过网关）
 */
@FeignClient(name = "unimall-service-goods")
public interface IGoodsClient
{
    @GetMapping("/goods/detail/{id}")
    Result<GoodsVO> detail(@PathVariable("id") Long id);

    @GetMapping("/goods/batch")
    Result<List<GoodsVO>> batch(@RequestParam("ids") List<Long> ids);
}

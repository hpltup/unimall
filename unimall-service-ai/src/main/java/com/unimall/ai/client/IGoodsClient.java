package com.unimall.ai.client;

import com.unimall.ai.pojo.AiPage;
import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务 Feign 客户端：搜索/详情（AI 工具调用用）
 */
@FeignClient(name = "unimall-service-goods")
public interface IGoodsClient
{
    @GetMapping("/goods/list")
    Result<AiPage<GoodsVO>> list(@RequestParam("pageNum") Integer pageNum,
                                 @RequestParam("pageSize") Integer pageSize,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "status", required = false) Integer status);

    @GetMapping("/goods/detail/{id}")
    Result<GoodsVO> detail(@PathVariable("id") Long id);
}

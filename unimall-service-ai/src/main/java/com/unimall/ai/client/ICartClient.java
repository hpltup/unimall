package com.unimall.ai.client;

import com.unimall.ai.dto.CartAddDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.CartItemVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * 购物车服务 Feign 客户端：加购/查看购物车（AI 工具调用用）
 */
@FeignClient(name = "unimall-service-cart")
public interface ICartClient
{
    @PostMapping("/cart/add")
    Result<Void> add(@RequestHeader("X-User-Id") Long userId, @RequestBody CartAddDTO dto);

    @GetMapping("/cart/list")
    Result<List<CartItemVO>> list(@RequestHeader("X-User-Id") Long userId);
}

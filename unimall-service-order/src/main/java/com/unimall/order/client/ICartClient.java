package com.unimall.order.client;

import com.unimall.common.result.Result;
import com.unimall.common.vo.CartItemVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * cart 服务 Feign 客户端（下单：取选中条目 / 清购物车）
 */
@FeignClient(name = "unimall-service-cart")
public interface ICartClient
{
    @GetMapping("/cart/internal/checked")
    Result<List<CartItemVO>> listChecked(@RequestParam("userId") Long userId);

    @PostMapping("/cart/internal/remove")
    Result<Void> removeBatch(@RequestParam("userId") Long userId, @RequestBody List<Long> ids);
}

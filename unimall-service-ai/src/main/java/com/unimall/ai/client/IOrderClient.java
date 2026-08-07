package com.unimall.ai.client;

import com.unimall.ai.pojo.AiPage;
import com.unimall.common.result.Result;
import com.unimall.common.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务 Feign 客户端：下单/查询/取消（AI 工具调用用）
 * 注：支付由用户在前端自行完成，AI 不代付，故未提供 pay 调用
 */
@FeignClient(name = "unimall-service-order")
public interface IOrderClient
{
    /** 购物车提交订单，返回订单ID */
    @PostMapping("/order/create")
    Result<Long> create(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/order/list")
    Result<AiPage<OrderVO>> list(@RequestHeader("X-User-Id") Long userId,
                                 @RequestParam("pageNum") Integer pageNum,
                                 @RequestParam("pageSize") Integer pageSize);

    @GetMapping("/order/detail/{id}")
    Result<OrderVO> detail(@RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long id);

    @PostMapping("/order/cancel/{id}")
    Result<Void> cancel(@RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long id);
}

package com.unimall.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.result.Result;
import com.unimall.order.pojo.vo.OrderVO;
import com.unimall.order.service.IOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController
{
    private final IOrderService orderService;

    public OrderController(IOrderService orderService)
    {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId)
    {
        return Result.ok(orderService.create(userId));
    }

    @GetMapping("/list")
    public Result<Page<OrderVO>> list(@RequestHeader("X-User-Id") Long userId,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return Result.ok(orderService.pageQuery(userId, pageNum, pageSize));
    }

    @GetMapping("/detail/{id}")
    public Result<OrderVO> detail(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id)
    {
        return Result.ok(orderService.detail(userId, id));
    }

    @PostMapping("/pay/{id}")
    public Result<Void> pay(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id)
    {
        orderService.pay(userId, id);
        return Result.ok();
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id)
    {
        orderService.cancel(userId, id);
        return Result.ok();
    }
}

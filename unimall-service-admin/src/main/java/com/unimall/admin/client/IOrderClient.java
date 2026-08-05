package com.unimall.admin.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.result.Result;
import com.unimall.common.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "unimall-service-order")
public interface IOrderClient
{
    @GetMapping("/order/internal/admin-list")
    Result<Page<OrderVO>> adminList(@RequestParam("pageNum") Integer pageNum,
                                    @RequestParam("pageSize") Integer pageSize,
                                    @RequestParam(value = "status", required = false) Integer status);

    @PostMapping("/order/internal/admin-ship/{id}")
    Result<Void> adminShip(@PathVariable("id") Long id);

    @PostMapping("/order/internal/admin-cancel/{id}")
    Result<Void> adminCancel(@PathVariable("id") Long id);
}

package com.unimall.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.admin.client.IOrderClient;
import com.unimall.common.result.Result;
import com.unimall.common.vo.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/order")
public class AdminOrderController
{
    private final IOrderClient orderClient;

    public AdminOrderController(IOrderClient orderClient)
    {
        this.orderClient = orderClient;
    }

    @GetMapping("/list")
    public Result<Page<OrderVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) Integer status)
    {
        return orderClient.adminList(pageNum, pageSize, status);
    }

    @PutMapping("/ship/{id}")
    public Result<Void> ship(@PathVariable Long id)
    {
        return orderClient.adminShip(id);
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id)
    {
        return orderClient.adminCancel(id);
    }
}

package com.unimall.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.order.pojo.vo.OrderVO;

public interface IOrderService
{
    /**
     * 从购物车选中条目下单（扣库存 → 建订单 → 清购物车），返回订单 id
     */
    Long create(Long userId);

    /**
     * 当前用户订单分页
     */
    Page<OrderVO> pageQuery(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 订单详情（含明细）
     */
    OrderVO detail(Long userId, Long id);

    /**
     * 模拟支付：待付款 → 已付款
     */
    void pay(Long userId, Long id);

    /**
     * 取消订单：待付款 → 已取消 + 恢复库存
     */
    void cancel(Long userId, Long id);
}

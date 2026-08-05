package com.unimall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.common.dto.GoodsStockDTO;
import com.unimall.common.exception.BusinessException;
import com.unimall.common.result.Result;
import com.unimall.common.vo.CartItemVO;
import com.unimall.order.client.ICartClient;
import com.unimall.order.client.IGoodsClient;
import com.unimall.order.mapper.IOrderItemMapper;
import com.unimall.order.mapper.IOrderMapper;
import com.unimall.order.pojo.entity.Order;
import com.unimall.order.pojo.entity.OrderItem;
import com.unimall.order.pojo.vo.OrderItemVO;
import com.unimall.order.pojo.vo.OrderVO;
import com.unimall.order.service.IOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl extends ServiceImpl<IOrderMapper, Order> implements IOrderService
{
    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_FINISHED = 2;
    private static final int STATUS_CANCELLED = 3;

    private final ICartClient cartClient;
    private final IGoodsClient goodsClient;
    private final IOrderItemMapper orderItemMapper;

    public OrderServiceImpl(ICartClient cartClient, IGoodsClient goodsClient, IOrderItemMapper orderItemMapper)
    {
        this.cartClient = cartClient;
        this.goodsClient = goodsClient;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId)
    {
        // 1. 取购物车选中条目
        Result<List<CartItemVO>> cartResult = cartClient.listChecked(userId);
        List<CartItemVO> items = cartResult != null && cartResult.getCode() == 0 ? cartResult.getData() : List.of();
        if (items == null || items.isEmpty())
        {
            throw new BusinessException(4003, "购物车没有选中的商品");
        }

        // 2. 逐条扣库存，失败则恢复已扣部分
        List<CartItemVO> deducted = new ArrayList<>();
        try
        {
            for (CartItemVO item : items)
            {
                GoodsStockDTO dto = new GoodsStockDTO();
                dto.setGoodsId(item.getGoodsId());
                dto.setQuantity(item.getQuantity());
                Result<Void> result = goodsClient.deductStock(dto);
                if (result == null || result.getCode() != 0)
                {
                    throw new BusinessException(4004, "商品库存不足");
                }
                deducted.add(item);
            }
        }
        catch (Exception e)
        {
            restoreDeducted(deducted);
            throw e;
        }

        // 3. 建订单 + 明细（本地事务）
        try
        {
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setStatus(STATUS_UNPAID);
            order.setTotalAmount(items.stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            save(order);

            for (CartItemVO item : items)
            {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setGoodsId(item.getGoodsId());
                orderItem.setGoodsName(item.getGoodsName());
                orderItem.setGoodsImage(item.getMainImage());
                orderItem.setPrice(item.getPrice());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                orderItemMapper.insert(orderItem);
            }

            // 4. 清购物车（失败不影响订单，仅记录）
            List<Long> cartIds = items.stream().map(CartItemVO::getId).toList();
            cartClient.removeBatch(userId, cartIds);

            return order.getId();
        }
        catch (Exception e)
        {
            restoreDeducted(deducted);
            throw e;
        }
    }

    @Override
    public Page<OrderVO> pageQuery(Long userId, Integer pageNum, Integer pageSize)
    {
        Page<Order> page = lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public OrderVO detail(Long userId, Long id)
    {
        Order order = getOrderOfUser(userId, id);
        return toVO(order);
    }

    @Override
    public void pay(Long userId, Long id)
    {
        Order order = getOrderOfUser(userId, id);
        if (order.getStatus() != STATUS_UNPAID)
        {
            throw new BusinessException(4002, "订单状态不允许支付");
        }
        lambdaUpdate()
                .eq(Order::getId, id)
                .set(Order::getStatus, STATUS_PAID)
                .set(Order::getPayTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void cancel(Long userId, Long id)
    {
        Order order = getOrderOfUser(userId, id);
        if (order.getStatus() != STATUS_UNPAID)
        {
            throw new BusinessException(4002, "订单状态不允许取消");
        }
        lambdaUpdate()
                .eq(Order::getId, id)
                .set(Order::getStatus, STATUS_CANCELLED)
                .update();

        // 恢复库存
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, id));
        for (OrderItem orderItem : orderItems)
        {
            GoodsStockDTO dto = new GoodsStockDTO();
            dto.setGoodsId(orderItem.getGoodsId());
            dto.setQuantity(orderItem.getQuantity());
            goodsClient.restoreStock(dto);
        }
    }

    private Order getOrderOfUser(Long userId, Long id)
    {
        Order order = lambdaQuery()
                .eq(Order::getId, id)
                .eq(Order::getUserId, userId)
                .one();
        if (order == null)
        {
            throw new BusinessException(4001, "订单不存在");
        }
        return order;
    }

    private void restoreDeducted(List<CartItemVO> deducted)
    {
        for (CartItemVO item : deducted)
        {
            GoodsStockDTO dto = new GoodsStockDTO();
            dto.setGoodsId(item.getGoodsId());
            dto.setQuantity(item.getQuantity());
            goodsClient.restoreStock(dto);
        }
    }

    private OrderVO toVO(Order order)
    {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(orderItems.stream().map(this::toItemVO).toList());
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem orderItem)
    {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(orderItem.getId());
        vo.setGoodsId(orderItem.getGoodsId());
        vo.setGoodsName(orderItem.getGoodsName());
        vo.setGoodsImage(orderItem.getGoodsImage());
        vo.setPrice(orderItem.getPrice());
        vo.setQuantity(orderItem.getQuantity());
        vo.setTotal(orderItem.getTotal());
        return vo;
    }

    private String generateOrderNo()
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}

package com.unimall.cart.service;

import com.unimall.cart.pojo.dto.CartAddDTO;
import com.unimall.cart.pojo.dto.CartQuantityDTO;
import com.unimall.common.vo.CartItemVO;

import java.util.List;

public interface ICartService
{
    /**
     * 加入购物车（已存在则累加数量）
     */
    void add(Long userId, CartAddDTO dto);

    /**
     * 当前用户购物车列表（含商品信息，经 Feign 查 goods 服务）
     */
    List<CartItemVO> list(Long userId);

    /**
     * 修改数量
     */
    void updateQuantity(Long userId, CartQuantityDTO dto);

    /**
     * 删除条目
     */
    void remove(Long userId, Long id);

    /**
     * 服务间调用：用户选中的购物车条目（下单用）
     */
    List<CartItemVO> listChecked(Long userId);

    /**
     * 服务间调用：批量删除指定条目（下单成功后清购物车）
     */
    void removeBatch(Long userId, List<Long> ids);
}

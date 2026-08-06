package com.unimall.cart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.cart.client.IGoodsClient;
import com.unimall.cart.mapper.ICartMapper;
import com.unimall.cart.pojo.dto.CartAddDTO;
import com.unimall.cart.pojo.dto.CartQuantityDTO;
import com.unimall.cart.pojo.entity.Cart;
import com.unimall.cart.service.ICartService;
import com.unimall.common.exception.BusinessException;
import com.unimall.common.result.Result;
import com.unimall.common.vo.CartItemVO;
import com.unimall.common.vo.GoodsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl extends ServiceImpl<ICartMapper, Cart> implements ICartService
{
    private final IGoodsClient goodsClient;

    public CartServiceImpl(IGoodsClient goodsClient)
    {
        this.goodsClient = goodsClient;
    }

    @Override
    public void add(Long userId, CartAddDTO dto)
    {
        // 校验商品存在且上架
        GoodsVO goods = getGoods(dto.getGoodsId());
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1)
        {
            throw new BusinessException(3001, "商品不存在或已下架");
        }

        // 已存在则累加数量，否则新增
        Cart cart = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getGoodsId, dto.getGoodsId())
                .one();
        if (cart != null)
        {
            lambdaUpdate()
                    .eq(Cart::getId, cart.getId())
                    .set(Cart::getQuantity, cart.getQuantity() + dto.getQuantity())
                    .update();
        }
        else
        {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setGoodsId(dto.getGoodsId());
            newCart.setQuantity(dto.getQuantity());
            newCart.setChecked(1);
            save(newCart);
        }
    }

    @Override
    public List<CartItemVO> list(Long userId)
    {
        List<Cart> carts = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getUpdateTime)
                .list();
        if (carts.isEmpty())
        {
            return List.of();
        }

        // 批量查商品信息（Feign）
        List<Long> goodsIds = carts.stream().map(Cart::getGoodsId).toList();
        Map<Long, GoodsVO> goodsMap = fetchGoods(goodsIds);

        return carts.stream().map(cart -> {
            CartItemVO vo = new CartItemVO();
            vo.setId(cart.getId());
            vo.setGoodsId(cart.getGoodsId());
            vo.setQuantity(cart.getQuantity());
            vo.setChecked(cart.getChecked());
            GoodsVO goods = goodsMap.get(cart.getGoodsId());
            if (goods != null)
            {
                vo.setGoodsName(goods.getName());
                vo.setMainImage(goods.getMainImage());
                vo.setPrice(goods.getPrice());
                vo.setTotal(goods.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            }
            return vo;
        }).toList();
    }

    @Override
    public void updateQuantity(Long userId, CartQuantityDTO dto)
    {
        getCartOfUser(userId, dto.getId());
        lambdaUpdate()
                .eq(Cart::getId, dto.getId())
                .set(Cart::getQuantity, dto.getQuantity())
                .update();
    }

    @Override
    public void remove(Long userId, Long id)
    {
        getCartOfUser(userId, id);
        baseMapper.deletePhysical(userId, List.of(id));
    }

    @Override
    public List<CartItemVO> listChecked(Long userId)
    {
        List<Cart> carts = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getChecked, 1)
                .list();
        if (carts.isEmpty())
        {
            return List.of();
        }

        List<Long> goodsIds = carts.stream().map(Cart::getGoodsId).toList();
        Map<Long, GoodsVO> goodsMap = fetchGoods(goodsIds);

        return carts.stream().map(cart -> {
            CartItemVO vo = new CartItemVO();
            vo.setId(cart.getId());
            vo.setGoodsId(cart.getGoodsId());
            vo.setQuantity(cart.getQuantity());
            vo.setChecked(cart.getChecked());
            GoodsVO goods = goodsMap.get(cart.getGoodsId());
            if (goods != null)
            {
                vo.setGoodsName(goods.getName());
                vo.setMainImage(goods.getMainImage());
                vo.setPrice(goods.getPrice());
                vo.setTotal(goods.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            }
            return vo;
        }).toList();
    }

    @Override
    public void removeBatch(Long userId, List<Long> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return;
        }
        // 物理删除（非逻辑删除）：避免 deleted=1 的记录占用唯一索引，导致用户再次加购同一商品时冲突
        baseMapper.deletePhysical(userId, ids);
    }

    private GoodsVO getGoods(Long goodsId)
    {
        Result<GoodsVO> result = goodsClient.detail(goodsId);
        return result != null && result.getCode() == 0 ? result.getData() : null;
    }

    private Map<Long, GoodsVO> fetchGoods(List<Long> goodsIds)
    {
        Result<List<GoodsVO>> result = goodsClient.batch(goodsIds);
        if (result == null || result.getCode() != 0 || result.getData() == null)
        {
            return new HashMap<>();
        }
        return result.getData().stream()
                .collect(Collectors.toMap(GoodsVO::getId, Function.identity(), (a, b) -> a));
    }

    private Cart getCartOfUser(Long userId, Long cartId)
    {
        Cart cart = lambdaQuery()
                .eq(Cart::getId, cartId)
                .eq(Cart::getUserId, userId)
                .one();
        if (cart == null)
        {
            throw new BusinessException(3002, "购物车条目不存在");
        }
        return cart;
    }
}

package com.unimall.cart.controller;

import com.unimall.cart.pojo.dto.CartAddDTO;
import com.unimall.cart.pojo.dto.CartQuantityDTO;
import com.unimall.cart.service.ICartService;
import com.unimall.common.result.Result;
import com.unimall.common.vo.CartItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController
{
    private final ICartService cartService;

    public CartController(ICartService cartService)
    {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestHeader("X-User-Id") Long userId, @RequestBody @Valid CartAddDTO dto)
    {
        cartService.add(userId, dto);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<CartItemVO>> list(@RequestHeader("X-User-Id") Long userId)
    {
        return Result.ok(cartService.list(userId));
    }

    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@RequestHeader("X-User-Id") Long userId, @RequestBody @Valid CartQuantityDTO dto)
    {
        cartService.updateQuantity(userId, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id)
    {
        cartService.remove(userId, id);
        return Result.ok();
    }

    /**
     * 服务间内部接口（订单服务调用，不走网关）：用户选中的购物车条目
     */
    @GetMapping("/internal/checked")
    public Result<List<CartItemVO>> listChecked(@RequestParam("userId") Long userId)
    {
        return Result.ok(cartService.listChecked(userId));
    }

    /**
     * 服务间内部接口（订单服务调用，不走网关）：批量删除条目
     */
    @PostMapping("/internal/remove")
    public Result<Void> removeBatch(@RequestParam("userId") Long userId, @RequestBody List<Long> ids)
    {
        cartService.removeBatch(userId, ids);
        return Result.ok();
    }
}

package com.unimall.goods.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.dto.GoodsStockDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import com.unimall.goods.pojo.dto.GoodsCreateDTO;
import com.unimall.goods.pojo.dto.GoodsQueryDTO;
import com.unimall.goods.pojo.dto.GoodsStatusDTO;
import com.unimall.goods.pojo.vo.GoodsVO;
import com.unimall.goods.service.IGoodsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/goods")
public class GoodsController
{
    private final IGoodsService goodsService;

    public GoodsController(IGoodsService goodsService)
    {
        this.goodsService = goodsService;
    }

    @GetMapping("/list")
    public Result<Page<GoodsVO>> list(GoodsQueryDTO dto)
    {
        return Result.ok(goodsService.pageQuery(dto));
    }

    @GetMapping("/detail/{id}")
    public Result<GoodsVO> detail(@PathVariable Long id)
    {
        return Result.ok(goodsService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid GoodsCreateDTO dto)
    {
        return Result.ok(goodsService.create(dto));
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody @Valid GoodsStatusDTO dto)
    {
        goodsService.updateStatus(dto.getId(), dto.getStatus());
        return Result.ok();
    }

    /**
     * 按 ids 批量查询（服务间调用内部接口，购物车/订单用）
     */
    @GetMapping("/batch")
    public Result<List<GoodsVO>> batch(@RequestParam("ids") List<Long> ids)
    {
        return Result.ok(goodsService.batchByIds(ids));
    }

    /**
     * 扣减库存（服务间调用内部接口，订单/秒杀用；原子防超卖）
     */
    @PostMapping("/deduct")
    public Result<Void> deductStock(@RequestBody @Valid GoodsStockDTO dto)
    {
        goodsService.deductStock(dto.getGoodsId(), dto.getQuantity());
        return Result.ok();
    }

    /**
     * 回补库存（服务间调用内部接口，订单取消时）
     */
    @PostMapping("/restore")
    public Result<Void> restoreStock(@RequestBody @Valid GoodsStockDTO dto)
    {
        goodsService.restoreStock(dto.getGoodsId(), dto.getQuantity());
        return Result.ok();
    }
}

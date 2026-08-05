package com.unimall.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.admin.client.IGoodsClient;
import com.unimall.common.dto.GoodsStatusDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/goods")
public class AdminGoodsController
{
    private final IGoodsClient goodsClient;

    public AdminGoodsController(IGoodsClient goodsClient)
    {
        this.goodsClient = goodsClient;
    }

    @GetMapping("/list")
    public Result<Page<GoodsVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status)
    {
        return goodsClient.list(pageNum, pageSize, keyword, status);
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody GoodsStatusDTO dto)
    {
        return goodsClient.updateStatus(dto);
    }
}

package com.unimall.admin.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimall.common.dto.GoodsStatusDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.GoodsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "unimall-service-goods")
public interface IGoodsClient
{
    @GetMapping("/goods/list")
    Result<Page<GoodsVO>> list(@RequestParam("pageNum") Integer pageNum,
                               @RequestParam("pageSize") Integer pageSize,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "status", required = false) Integer status);

    @PutMapping("/goods/status")
    Result<Void> updateStatus(@RequestBody GoodsStatusDTO dto);
}

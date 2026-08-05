package com.unimall.admin.client;

import com.unimall.common.dto.SeckillActivityCreateDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.SeckillActivityVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "unimall-service-seckill")
public interface ISeckillClient
{
    @PostMapping("/seckill/activity")
    Result<Long> createActivity(@RequestBody SeckillActivityCreateDTO dto);

    @GetMapping("/seckill/list")
    Result<List<SeckillActivityVO>> list();
}

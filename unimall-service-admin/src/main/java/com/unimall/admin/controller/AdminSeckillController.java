package com.unimall.admin.controller;

import com.unimall.admin.client.ISeckillClient;
import com.unimall.common.dto.SeckillActivityCreateDTO;
import com.unimall.common.result.Result;
import com.unimall.common.vo.SeckillActivityVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/seckill")
public class AdminSeckillController
{
    private final ISeckillClient seckillClient;

    public AdminSeckillController(ISeckillClient seckillClient)
    {
        this.seckillClient = seckillClient;
    }

    @PostMapping("/activity")
    public Result<Long> createActivity(@RequestBody SeckillActivityCreateDTO dto)
    {
        return seckillClient.createActivity(dto);
    }

    @GetMapping("/list")
    public Result<List<SeckillActivityVO>> list()
    {
        return seckillClient.list();
    }
}

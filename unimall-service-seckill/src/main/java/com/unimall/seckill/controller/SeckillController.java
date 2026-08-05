package com.unimall.seckill.controller;

import com.unimall.common.result.Result;
import com.unimall.seckill.pojo.dto.SeckillActivityCreateDTO;
import com.unimall.seckill.pojo.vo.SeckillActivityVO;
import com.unimall.seckill.pojo.vo.SeckillOrderVO;
import com.unimall.seckill.service.ISeckillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seckill")
public class SeckillController
{
    private final ISeckillService seckillService;

    public SeckillController(ISeckillService seckillService)
    {
        this.seckillService = seckillService;
    }

    /**
     * 创建秒杀活动（管理接口）
     */
    @PostMapping("/activity")
    public Result<Long> createActivity(@RequestBody @Valid SeckillActivityCreateDTO dto)
    {
        return Result.ok(seckillService.createActivity(dto));
    }

    @GetMapping("/list")
    public Result<List<SeckillActivityVO>> list()
    {
        return Result.ok(seckillService.listActivities());
    }

    @GetMapping("/detail/{id}")
    public Result<SeckillActivityVO> detail(@PathVariable Long id)
    {
        return Result.ok(seckillService.detail(id));
    }

    /**
     * 抢购（需登录）
     */
    @PostMapping("/{activityId}")
    public Result<String> doSeckill(@RequestHeader("X-User-Id") Long userId, @PathVariable Long activityId)
    {
        return Result.ok(seckillService.doSeckill(userId, activityId));
    }

    /**
     * 查询秒杀结果（同步方案抢购成功即返回；MQ 演进后前端轮询此接口）
     */
    @GetMapping("/result/{orderNo}")
    public Result<SeckillOrderVO> result(@PathVariable String orderNo)
    {
        return Result.ok(seckillService.result(orderNo));
    }
}

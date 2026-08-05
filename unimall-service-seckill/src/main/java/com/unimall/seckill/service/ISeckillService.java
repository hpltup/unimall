package com.unimall.seckill.service;

import com.unimall.seckill.pojo.dto.SeckillActivityCreateDTO;
import com.unimall.seckill.pojo.vo.SeckillActivityVO;
import com.unimall.seckill.pojo.vo.SeckillOrderVO;

import java.util.List;

public interface ISeckillService
{
    /**
     * 创建秒杀活动，返回活动 id
     */
    Long createActivity(SeckillActivityCreateDTO dto);

    /**
     * 活动列表（未开始 + 进行中）
     */
    List<SeckillActivityVO> listActivities();

    /**
     * 活动详情
     */
    SeckillActivityVO detail(Long id);

    /**
     * 抢购：Redis Lua 原子扣库存 + 限购 → 建秒杀订单，返回订单号
     */
    String doSeckill(Long userId, Long activityId);

    /**
     * 查询秒杀结果（按订单号；同步方案抢购成功即存在，MQ 演进后由前端轮询）
     */
    SeckillOrderVO result(String orderNo);
}

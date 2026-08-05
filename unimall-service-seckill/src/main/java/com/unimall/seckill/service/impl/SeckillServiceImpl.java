package com.unimall.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimall.common.dto.SeckillActivityCreateDTO;
import com.unimall.common.exception.BusinessException;
import com.unimall.common.vo.SeckillActivityVO;
import com.unimall.seckill.mapper.ISeckillActivityMapper;
import com.unimall.seckill.mapper.ISeckillOrderMapper;
import com.unimall.seckill.pojo.entity.SeckillActivity;
import com.unimall.seckill.pojo.entity.SeckillOrder;
import com.unimall.seckill.pojo.vo.SeckillOrderVO;
import com.unimall.seckill.service.ISeckillService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SeckillServiceImpl extends ServiceImpl<ISeckillActivityMapper, SeckillActivity> implements ISeckillService
{
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String LIMIT_KEY_PREFIX = "seckill:limit:";

    /**
     * Lua 原子扣减：库存 > 0 且未超限购才扣减，返回 1 成功 / -1 库存不足 / -2 超限购
     * KEYS[1] = 库存 key，KEYS[2] = 限购 key，ARGV[1] = 限购数量
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>(
            "local stock = tonumber(redis.call('GET', KEYS[1]) or '0') "
                    + "local bought = tonumber(redis.call('GET', KEYS[2]) or '0') "
                    + "if stock <= 0 then return -1 end "
                    + "if bought >= tonumber(ARGV[1]) then return -2 end "
                    + "redis.call('DECR', KEYS[1]) "
                    + "redis.call('INCR', KEYS[2]) "
                    + "return 1",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ISeckillOrderMapper orderMapper;

    public SeckillServiceImpl(StringRedisTemplate redisTemplate, ISeckillOrderMapper orderMapper)
    {
        this.redisTemplate = redisTemplate;
        this.orderMapper = orderMapper;
    }

    @Override
    public Long createActivity(SeckillActivityCreateDTO dto)
    {
        SeckillActivity activity = new SeckillActivity();
        activity.setGoodsId(dto.getGoodsId());
        activity.setGoodsName(dto.getGoodsName());
        activity.setGoodsImage(dto.getGoodsImage());
        activity.setSeckillPrice(dto.getSeckillPrice());
        activity.setStock(dto.getStock());
        activity.setLimitPerUser(dto.getLimitPerUser());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setStatus(0);
        save(activity);
        return activity.getId();
    }

    @Override
    public List<SeckillActivityVO> listActivities()
    {
        LocalDateTime now = LocalDateTime.now();
        return lambdaQuery()
                .ge(SeckillActivity::getEndTime, now)
                .orderByAsc(SeckillActivity::getStartTime)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public SeckillActivityVO detail(Long id)
    {
        SeckillActivity activity = getById(id);
        if (activity == null)
        {
            throw new BusinessException(5001, "秒杀活动不存在");
        }
        return toVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String doSeckill(Long userId, Long activityId)
    {
        SeckillActivity activity = getById(activityId);
        if (activity == null)
        {
            throw new BusinessException(5001, "秒杀活动不存在");
        }

        // 时间校验
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()))
        {
            throw new BusinessException(5002, "秒杀活动未开始");
        }
        if (now.isAfter(activity.getEndTime()))
        {
            throw new BusinessException(5002, "秒杀活动已结束");
        }

        // 懒加载预热：库存 key 不存在时从 DB 写入（setIfAbsent 幂等）
        String stockKey = STOCK_KEY_PREFIX + activityId;
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(activity.getStock()));

        // Lua 原子扣库存 + 限购校验
        String limitKey = LIMIT_KEY_PREFIX + activityId + ":" + userId;
        Long result = redisTemplate.execute(
                SECKILL_SCRIPT,
                List.of(stockKey, limitKey),
                String.valueOf(activity.getLimitPerUser()));
        if (result == null || result == -1)
        {
            throw new BusinessException(5003, "已被抢光");
        }
        if (result == -2)
        {
            throw new BusinessException(5004, "超过每人限购数量");
        }

        // 建秒杀订单（独立方法：MQ 演进时由消费者调用）
        return createSeckillOrder(activity, userId);
    }

    @Override
    public SeckillOrderVO result(String orderNo)
    {
        SeckillOrder order = orderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getOrderNo, orderNo));
        if (order == null)
        {
            throw new BusinessException(5005, "暂无秒杀结果");
        }
        return toOrderVO(order);
    }

    /**
     * 创建秒杀订单（独立封装：后续切换 RabbitMQ 异步削峰时，由消费者调用本方法）
     */
    private String createSeckillOrder(SeckillActivity activity, Long userId)
    {
        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(generateOrderNo());
        order.setActivityId(activity.getId());
        order.setUserId(userId);
        order.setGoodsId(activity.getGoodsId());
        order.setGoodsName(activity.getGoodsName());
        order.setGoodsImage(activity.getGoodsImage());
        order.setSeckillPrice(activity.getSeckillPrice());
        order.setQuantity(1);
        order.setTotal(activity.getSeckillPrice());
        order.setStatus(0);
        orderMapper.insert(order);
        return order.getOrderNo();
    }

    private SeckillActivityVO toVO(SeckillActivity activity)
    {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setGoodsId(activity.getGoodsId());
        vo.setGoodsName(activity.getGoodsName());
        vo.setGoodsImage(activity.getGoodsImage());
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setStock(activity.getStock());
        vo.setLimitPerUser(activity.getLimitPerUser());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(calcStatus(activity));
        return vo;
    }

    private int calcStatus(SeckillActivity activity)
    {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()))
        {
            return 0;
        }
        if (now.isAfter(activity.getEndTime()))
        {
            return 2;
        }
        return 1;
    }

    private SeckillOrderVO toOrderVO(SeckillOrder order)
    {
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setActivityId(order.getActivityId());
        vo.setGoodsId(order.getGoodsId());
        vo.setGoodsName(order.getGoodsName());
        vo.setGoodsImage(order.getGoodsImage());
        vo.setSeckillPrice(order.getSeckillPrice());
        vo.setQuantity(order.getQuantity());
        vo.setTotal(order.getTotal());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private String generateOrderNo()
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}

package com.unimall.sendmsg.service.impl;

import com.unimall.common.exception.BusinessException;
import com.unimall.sendmsg.service.ISmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SmsServiceImpl implements ISmsService
{
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long CODE_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;

    public SmsServiceImpl(StringRedisTemplate redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void send(String phone)
    {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, Duration.ofSeconds(CODE_TTL_SECONDS));
        // 模拟发送：未接入真实短信服务商，生产环境替换为阿里云/腾讯云短信 SDK
        logger.info("[模拟短信] 向 {} 发送验证码：{}（5分钟内有效）", phone, code);
    }

    @Override
    public void verify(String phone, String code)
    {
        String saved = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
        if (saved == null)
        {
            throw new BusinessException(8003, "验证码已过期，请重新获取");
        }
        if (!saved.equals(code))
        {
            throw new BusinessException(8002, "验证码错误");
        }
        // 一次性：校验通过即删除
        redisTemplate.delete(SMS_CODE_PREFIX + phone);
    }
}

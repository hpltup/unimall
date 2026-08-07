package com.unimall.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 客服业务配置（对应配置中心 ai-dev.yml 的 unimall.ai 前缀）
 */
@Component
@ConfigurationProperties(prefix = "unimall.ai")
public class AiProperties
{
    /** 会话上下文在 Redis 中的存活时间（秒），默认 2 小时 */
    private long sessionTtlSeconds = 7200;

    /** 单会话保留的最大消息条数（超出丢弃最旧），防止上下文过长 */
    private int maxMessages = 20;

    public long getSessionTtlSeconds()
    {
        return sessionTtlSeconds;
    }

    public void setSessionTtlSeconds(long sessionTtlSeconds)
    {
        this.sessionTtlSeconds = sessionTtlSeconds;
    }

    public int getMaxMessages()
    {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages)
    {
        this.maxMessages = maxMessages;
    }
}

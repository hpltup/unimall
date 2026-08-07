package com.unimall.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 客服服务启动类
 * <p>
 * 基于 Spring AI（OpenAI 兼容客户端，模型 DeepSeek）实现智能客服：
 * 对话、商品推荐/搜索、购物车与订单操作（Function Calling）。
 */
@SpringBootApplication
@EnableFeignClients
public class AiApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AiApplication.class, args);
    }
}

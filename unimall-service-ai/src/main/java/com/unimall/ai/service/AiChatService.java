package com.unimall.ai.service;

import reactor.core.publisher.Flux;

/**
 * AI 对话服务：会话上下文管理 + 流式对话
 */
public interface AiChatService
{
    /**
     * 流式对话：基于 Redis 中的会话历史调用大模型，工具调用在流式过程中自动完成，
     * 返回模型增量文本流；流结束后把本轮消息回写会话历史
     *
     * @param userId    用户ID（来自网关 X-User-Id）
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 模型回复的增量文本流
     */
    Flux<String> chatStream(Long userId, String sessionId, String message);
}

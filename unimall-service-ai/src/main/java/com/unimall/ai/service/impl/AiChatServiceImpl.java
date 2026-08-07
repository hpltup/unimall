package com.unimall.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimall.ai.client.ICartClient;
import com.unimall.ai.client.IGoodsClient;
import com.unimall.ai.client.IOrderClient;
import com.unimall.ai.config.AiProperties;
import com.unimall.ai.pojo.ChatMessage;
import com.unimall.ai.service.AiChatService;
import com.unimall.ai.tools.AiTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话服务实现：Redis 会话历史 + Spring AI 流式对话
 * <p>
 * 工具调用（Function Calling）通过每次请求 new 一个绑定 userId 的 AiTools 实例注入
 * ChatClient，避免 ThreadLocal 跨线程丢身份的问题。
 */
@Service
public class AiChatServiceImpl implements AiChatService
{
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);
    private static final String SESSION_KEY_PREFIX = "chat:session:";

    private final ChatClient chatClient;
    private final IGoodsClient goodsClient;
    private final ICartClient cartClient;
    private final IOrderClient orderClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public AiChatServiceImpl(ChatClient chatClient,
                             IGoodsClient goodsClient,
                             ICartClient cartClient,
                             IOrderClient orderClient,
                             StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             AiProperties aiProperties)
    {
        this.chatClient = chatClient;
        this.goodsClient = goodsClient;
        this.cartClient = cartClient;
        this.orderClient = orderClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
    }

    @Override
    public Flux<String> chatStream(Long userId, String sessionId, String message)
    {
        String key = SESSION_KEY_PREFIX + userId + ":" + sessionId;

        // 1. 加载历史并追加用户消息（截断到上限）
        List<ChatMessage> history = loadHistory(key);
        history.add(new ChatMessage("user", message));
        trimHistory(history);

        // 2. 组装 AI 消息列表
        List<Message> aiMessages = new ArrayList<>(history.size());
        for (ChatMessage chatMessage : history)
        {
            if ("user".equals(chatMessage.getRole()))
            {
                aiMessages.add(new UserMessage(chatMessage.getContent()));
            }
            else if ("assistant".equals(chatMessage.getRole()))
            {
                aiMessages.add(new AssistantMessage(chatMessage.getContent()));
            }
        }

        // 3. 绑定用户身份的工具实例（每请求新建，规避异步线程丢身份）
        AiTools tools = new AiTools(userId, goodsClient, cartClient, orderClient);
        ChatClient sessionChatClient = chatClient.mutate().defaultTools(tools).build();

        // 4. 流式对话，流结束回写会话历史
        StringBuilder reply = new StringBuilder();
        return sessionChatClient.prompt()
                .messages(aiMessages)
                .stream()
                .content()
                .doOnNext(reply::append)
                .doOnComplete(() ->
                {
                    history.add(new ChatMessage("assistant", reply.toString()));
                    saveHistory(key, history);
                });
    }

    private List<ChatMessage> loadHistory(String key)
    {
        try
        {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null)
            {
                return new ArrayList<>();
            }
            List<ChatMessage> history = objectMapper.readValue(json, new TypeReference<List<ChatMessage>>()
            {
            });
            return history == null ? new ArrayList<>() : history;
        }
        catch (Exception e)
        {
            log.warn("读取会话历史失败，key={}", key, e);
            return new ArrayList<>();
        }
    }

    private void saveHistory(String key, List<ChatMessage> history)
    {
        try
        {
            String json = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(aiProperties.getSessionTtlSeconds()));
        }
        catch (Exception e)
        {
            log.warn("保存会话历史失败，key={}", key, e);
        }
    }

    private void trimHistory(List<ChatMessage> history)
    {
        int max = aiProperties.getMaxMessages();
        while (history.size() > max)
        {
            history.remove(0);
        }
    }
}

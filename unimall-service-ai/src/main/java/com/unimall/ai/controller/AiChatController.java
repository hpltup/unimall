package com.unimall.ai.controller;

import com.unimall.ai.dto.ChatRequestDTO;
import com.unimall.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * AI 客服对话接口（SSE 流式）
 * <p>
 * 网关路由 /api/ai/** → /ai/**，需登录，用户身份来自 X-User-Id 头。
 * 响应头 X-Session-Id：新会话时生成，前端需保存并在下一轮请求中携带。
 */
@RestController
@RequestMapping("/ai")
public class AiChatController
{
    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService)
    {
        this.aiChatService = aiChatService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> chat(@RequestHeader("X-User-Id") Long userId,
                                                              @RequestBody @Valid ChatRequestDTO dto)
    {
        String sessionId = (dto.getSessionId() == null || dto.getSessionId().isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : dto.getSessionId();

        Flux<ServerSentEvent<String>> sse = aiChatService.chatStream(userId, sessionId, dto.getMessage())
                .map(chunk -> ServerSentEvent.<String>builder(chunk).build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder("[DONE]").build()));

        return ResponseEntity.ok()
                .header("X-Session-Id", sessionId)
                .body(sse);
    }
}

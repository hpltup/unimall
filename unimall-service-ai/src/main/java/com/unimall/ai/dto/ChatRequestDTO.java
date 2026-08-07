package com.unimall.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求入参
 */
@Data
public class ChatRequestDTO
{
    /**
     * 会话 ID：为空时服务端创建新会话（通过响应头 X-Session-Id 返回）；
     * 后续轮次携带以延续多轮上下文
     */
    @Size(max = 64, message = "会话ID过长")
    private String sessionId;

    /** 用户消息 */
    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息过长")
    private String message;
}

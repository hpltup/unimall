package com.unimall.ai.pojo;

import lombok.Data;

/**
 * 会话消息（Redis 中存 JSON 数组：[{role, content}, ...]）
 */
@Data
public class ChatMessage
{
    /** 角色：user / assistant */
    private String role;

    private String content;

    public ChatMessage()
    {
    }

    public ChatMessage(String role, String content)
    {
        this.role = role;
        this.content = content;
    }
}

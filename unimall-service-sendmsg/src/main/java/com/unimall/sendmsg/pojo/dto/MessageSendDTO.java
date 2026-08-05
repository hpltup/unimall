package com.unimall.sendmsg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageSendDTO
{
    @NotNull(message = "收件人不能为空")
    private Long userId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 1000, message = "内容不能超过1000字")
    private String content;
}

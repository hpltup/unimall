package com.unimall.sendmsg.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO
{
    private Long id;

    private String title;

    private String content;

    private Integer isRead;

    private LocalDateTime createTime;
}

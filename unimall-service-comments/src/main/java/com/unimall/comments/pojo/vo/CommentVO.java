package com.unimall.comments.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO
{
    private Long id;

    private Long goodsId;

    private Long userId;

    private String content;

    private String images;

    private Integer rating;

    private LocalDateTime createTime;
}

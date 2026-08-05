package com.unimall.comments.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    private Long userId;

    private String content;

    private String images;

    /** 评分 1~5 */
    private Integer rating;

    /** 0待审核 1显示 2隐藏 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

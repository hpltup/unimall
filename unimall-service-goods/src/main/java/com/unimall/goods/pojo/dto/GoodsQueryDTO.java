package com.unimall.goods.pojo.dto;

import lombok.Data;

@Data
public class GoodsQueryDTO
{
    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 分类ID */
    private Long categoryId;

    /** 关键词（模糊匹配商品名/副标题） */
    private String keyword;

    /** 状态：0下架 1上架（为空查全部） */
    private Integer status;
}

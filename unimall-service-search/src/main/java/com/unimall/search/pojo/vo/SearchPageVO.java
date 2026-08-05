package com.unimall.search.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索分页结果（字段名与 MyBatis-Plus Page 一致，前端无感）
 */
@Data
public class SearchPageVO<T>
{
    private List<T> records;

    private long total;

    private long current;

    private long size;
}

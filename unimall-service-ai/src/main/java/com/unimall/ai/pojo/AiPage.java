package com.unimall.ai.pojo;

import lombok.Data;

import java.util.List;

/**
 * 轻量分页出参：仅用于 Feign 反序列化各服务分页接口返回
 * （字段与 MyBatis-Plus Page 的序列化字段一一对应，AI 模块不引入 MP 依赖）
 */
@Data
public class AiPage<T>
{
    private List<T> records;

    private long total;

    private long size;

    private long current;

    private long pages;
}

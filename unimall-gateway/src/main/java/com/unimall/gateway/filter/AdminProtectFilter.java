package com.unimall.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimall.common.result.Result;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 管理/内部接口保护：普通用户经网关访问管理操作或服务间内部接口时直接拒绝（403）。
 * 管理操作（商品新增/上下架、秒杀建活动）与内部接口（/internal/**、/goods/batch|deduct|restore）
 * 仅允许服务间 Feign 直连（admin/order/cart 调用），不经过网关，因此不受本过滤器影响。
 */
@Component
public class AdminProtectFilter implements GlobalFilter, Ordered
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        boolean blocked = false;

        // 1. 所有服务的内部接口（/internal/**）不经网关
        if (path.startsWith("/api/") && path.contains("/internal/"))
        {
            blocked = true;
        }
        // 2. 商品管理：新增（POST /api/goods）、上下架（PUT /api/goods/status）
        else if (path.equals("/api/goods") && method == HttpMethod.POST)
        {
            blocked = true;
        }
        else if (path.equals("/api/goods/status") && method == HttpMethod.PUT)
        {
            blocked = true;
        }
        // 3. 商品库存内部接口：批量查/扣库存/回补（仅服务间调用）
        else if (path.equals("/api/goods/batch")
                || path.equals("/api/goods/deduct")
                || path.equals("/api/goods/restore"))
        {
            blocked = true;
        }
        // 4. 秒杀管理：创建活动（POST /api/seckill/activity）；抢购 POST /api/seckill/{id} 保留
        else if (path.equals("/api/seckill/activity") && method == HttpMethod.POST)
        {
            blocked = true;
        }

        if (blocked)
        {
            return forbidden(exchange.getResponse());
        }
        return chain.filter(exchange);
    }

    private Mono<Void> forbidden(ServerHttpResponse response)
    {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try
        {
            bytes = objectMapper.writeValueAsBytes(Result.fail(403, "禁止访问：管理操作请通过后台管理接口"));
        }
        catch (Exception e)
        {
            bytes = "{\"code\":403,\"message\":\"禁止访问\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder()
    {
        // 在鉴权过滤器（-100）之前执行
        return -200;
    }
}

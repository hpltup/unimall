# unimall-service-ai — AI 客服服务

AI 客服服务（端口 **10022**）：基于 Spring AI（OpenAI 兼容协议，模型 DeepSeek）实现智能对话、商品推荐/加购、购物车与订单操作（Function Calling）。**不落库**（会话存 Redis，无 MySQL）。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /ai/chat`（SSE 流式） | 请求 `{sessionId?, message}`；响应头 `X-Session-Id`（新会话时生成，前端保存后续传）；响应体 `text/event-stream`：`data:` 增量文本，结束发 `data: [DONE]` | 需登录（网关 `/api/ai/**`，透传 `X-User-Id`） |

## 分层

```
com.unimall.ai/
├── AiApplication.java             # @SpringBootApplication + @EnableFeignClients
├── controller/AiChatController.java   # POST /ai/chat → SSE + X-Session-Id 头
├── dto/                           # ChatRequestDTO（{sessionId?, message}）、CartAddDTO（加购 Feign 用）
├── service/AiChatService.java + impl/AiChatServiceImpl.java  # Redis 会话 + 流式编排
├── tools/AiTools.java             # @Tool 工具集（每请求 new，构造器绑定 userId）
├── client/                        # IGoodsClient / ICartClient / IOrderClient（Feign 直连，透传 X-User-Id）
├── pojo/                          # AiPage<T>（轻量分页）、ChatMessage（会话消息）
└── config/
    ├── ChatClientConfig.java      # @Bean ChatClient + 中文 system prompt
    ├── AiProperties.java          # @ConfigurationProperties(unimall.ai.*)：sessionTtlSeconds/maxMessages
    └── GlobalExceptionHandler.java
```

## 要点

- **工具集**（`@Tool`）：`searchGoods(keyword)` / `getGoodsDetail(id)` / `addToCart(goodsId, quantity)` / `getCart` / `createOrder` / `getOrders` / `getOrderDetail(id)` / `cancelOrder(id)`；内部经 Feign 直连 goods/cart/order（不走网关）
- **支付不代付**：无支付工具，system prompt 约束"下单成功后引导用户在订单中心自行支付"
- **下单安全**：system prompt 强制"先展示购物车 → 用户明确确认 → 才允许下单"；取消订单同理
- **身份传递**：`AiTools` 每请求实例化（构造器注入 userId + Feign 客户端），经 `ChatClient.mutate().defaultTools()` 绑定，规避 ThreadLocal 跨异步线程丢身份
- **会话**：Redis `chat:session:{userId}:{sessionId}` 存 JSON 消息数组，TTL 7200s、上限 20 条（丢最旧），流结束回写
- **无 MyBatis-Plus / MySQL**：Feign 分页返回用模块内 `AiPage<T>`（字段与 MP Page 序列化一致）反序列化
- **配置**（配置中心 `ai-dev.yml`）：`spring.ai.openai.base-url: https://api.deepseek.com`、`api-key: ${DEEPSEEK_API_KEY:...}`（环境变量注入，禁止提交真实密钥）、`model: deepseek-chat`；`unimall.ai.session-ttl-seconds / max-messages`
- 错误码：沿用全局约定（BusinessException → Result；5000 系统异常）

## 已知问题

- 会话无用户主动清空接口（Redis TTL 自动过期）
- 大模型调用为同步长连接，无流式中断/超时兜底，前端断开时后端仍会跑完一轮
- 工具调用无额外风控（依赖 prompt 约束），生产可加：下单二次 token 确认、额度/频次限制
- 若 DeepSeek 调用 404：base-url 试改 `https://api.deepseek.com/v1`

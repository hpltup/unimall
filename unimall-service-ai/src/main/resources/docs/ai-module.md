# unimall-service-ai AI 客服模块说明

> 智能客服服务：基于 Spring AI（OpenAI 兼容协议，模型 DeepSeek）实现对话、商品推荐、购物车与订单操作（Function Calling）。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、接口清单](#三接口清单)
- [四、功能实现思路](#四功能实现思路)
- [五、目录结构](#五目录结构)
- [六、配置说明](#六配置说明)
- [七、启动前提与已知问题](#七启动前提与已知问题)

---

## 一、模块定位

AI 客服服务（端口 **10022**）：用户可与客服对话，AI 通过工具调用完成商品搜索/推荐、查看购物车、下单、订单查询/支付/取消。**不落库**（会话存 Redis，无 MySQL）。

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / Redis / OpenFeign / **Spring AI 1.0.0**（`spring-ai-starter-model-openai`，Maven Central 正式版）→ DeepSeek `deepseek-chat`。

- **无 MyBatis-Plus / MySQL**：会话不落库；Feign 分页返回用模块内轻量 `AiPage<T>`（字段与 MP Page 序列化一致）反序列化，避免引入 MP 依赖
- Spring AI 版本由根 pom `spring-ai-bom`（1.0.0）统一管理

## 三、接口清单

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /ai/chat`（SSE 流式） | 请求 `{sessionId?, message}`；响应头 `X-Session-Id`（新会话时生成）；响应体 `text/event-stream`，`data:` 增量文本，结束发 `data: [DONE]` | **需登录** |

网关路由 `/api/ai/**` → `lb://unimall-service-ai`；**不在白名单**，鉴权后透传 `X-User-Id`。

## 四、功能实现思路

```
用户消息 → Redis 取会话历史（chat:session:{userId}:{sessionId}，TTL 2h，上限 20 条）
  → 追加用户消息 → 组装 Message 列表
  → new AiTools(userId, ...) 绑定身份 → chatClient.mutate().defaultTools(tools).build()
  → prompt().messages(...).stream().content()（工具调用在流式内部自动循环）
  → 增量文本 SSE 回前端 → 流结束把完整回复回写 Redis 历史
```

**工具集（`@Tool`）**：`searchGoods(keyword)` / `getGoodsDetail(id)` / `addToCart(goodsId, quantity)` / `getCart` / `createOrder` / `getOrders` / `getOrderDetail(id)` / `cancelOrder(id)` —— 内部经 Feign 直连 cart/goods/order 服务（不走网关）。**不含支付**：支付由用户在前端完成，AI 只做引导（下单成功后提醒到订单中心自行支付）。

**关键设计**：
1. **身份传递**：`AiTools` 每请求 new（构造器注入 userId + Feign 客户端），经 `ChatClient.mutate().defaultTools()` 绑定，规避 ThreadLocal 跨异步线程丢身份问题
2. **下单安全**：system prompt 强制"先展示购物车 → 用户明确确认 → 才允许下单"；取消订单同理；工具异常时如实转述
3. **支付不代付**：AI 无支付工具，下单后引导用户在订单中心完成支付（资金操作必须由用户在前端发起）
4. **流式 + 工具**：Spring AI 自动完成"模型→工具→再输出"循环，最终只回文本流

## 五、目录结构

```
com.unimall.ai/
├── AiApplication.java             # @SpringBootApplication + @EnableFeignClients
├── controller/AiChatController.java  # POST /ai/chat → SSE + X-Session-Id
├── dto/ChatRequestDTO.java        # {sessionId?, message}
├── service/AiChatService.java + impl/AiChatServiceImpl.java  # 会话管理 + 流式编排
├── tools/AiTools.java             # @Tool 工具集（每请求实例化，绑定 userId）
├── client/                        # IGoodsClient / ICartClient / IOrderClient
├── pojo/                          # AiPage<T>（轻量分页）、ChatMessage（会话消息）
└── config/
    ├── ChatClientConfig.java      # @Bean ChatClient + 中文 system prompt
    ├── AiProperties.java          # unimall.ai.*（会话 TTL / 消息上限）
    └── GlobalExceptionHandler.java
```

## 六、配置说明

- 本地 `application.yml`：端口 10022、Nacos、config 客户端
- 配置中心 `ai-dev.yml`（gitee `unimall-config-dev/ai-dev/` + config-repo 同步）：
  - `spring.data.redis`：会话存储
  - `spring.ai.openai`：`base-url: https://api.deepseek.com`、`api-key: ${DEEPSEEK_API_KEY:...}`（**环境变量注入，禁止提交真实密钥**）、`model: deepseek-chat`
  - `unimall.ai`：`session-ttl-seconds: 7200`、`max-messages: 20`

## 七、启动前提与已知问题

- 前提：Nacos、config、Redis 启动；**DEEPSEEK_API_KEY 环境变量**（DeepSeek 开放平台申请）；本地 Maven 已刷新 `spring-ai-bom` 依赖
- 已知问题/后续演进：
  - 会话无用户主动清空接口（Redis TTL 自动过期）
  - 大模型调用为同步长连接，未做流式中断/超时兜底，前端断开时后端仍会跑完一轮
  - 工具调用无额外风控（依赖 prompt 约束），生产可加：下单/支付二次 token 确认、额度/频次限制
  - SSE 经网关已验证方案可行，实测建议用 `curl -N` / Apifox 流式模式验证

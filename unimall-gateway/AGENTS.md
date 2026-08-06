# unimall-gateway — 网关

统一入口（端口 **10011**）：服务路由、JWT 鉴权（Redis 白名单）、CORS。

## 硬性约束

- **WebFlux 应用：禁止引入 `spring-boot-starter-web`**（启动冲突）
- 路由 / CORS / 白名单等业务配置**不在本地**，在配置中心 `gateway-dev.yml`（本地 `application.yml` 只留引导配置）
- 已引 `spring-cloud-starter-loadbalancer`（`lb://` 路由必需，2023 系无 Ribbon）

## 鉴权（`filter/AuthGlobalFilter.java`，order=-100）

流程：白名单命中（`unimall.gateway.auth.whitelist`）→ 放行；否则取 `Authorization: Bearer <token>` → `JwtUtil.parseToken` 验签 → `ReactiveStringRedisTemplate` 查 `login:token:{jti}` → 存在放行并附加 **`X-User-Id`** 头 / 401 JSON。

- Redis 用 **`ReactiveStringRedisTemplate`**（WebFlux 版）
- **key 前缀 `login:token:` 必须与 user 服务 `UserServiceImpl` 保持一致**
- 密钥来自配置中心共享 `application.yml` 的 `unimall.jwt.secret`（占位符，生产用环境变量 `JWT_SECRET`）

## 管理/内部接口保护（`filter/AdminProtectFilter.java`，order=-200）

普通用户经网关访问管理操作或内部接口 → 403：`POST /api/goods`、`PUT /api/goods/status`、`/api/*/internal/**`、`/api/goods/batch|deduct|restore`、`POST /api/seckill/activity`。管理操作只能走 `/api/admin/*`（admin 服务 Feign 直连，不经网关，不受影响）。

## Bus 动态刷新

已接入 `spring-cloud-starter-bus-amqp` + actuator：改 `gateway-dev.yml` → push gitee → `POST /actuator/busrefresh` → 网关路由热加载（不重启）。

## 路由约定

`/api/{服务}/**` + `StripPrefix=1`（`/api/user/login` → 服务端 `/user/login`）。已配 10 条：user/goods/order/cart/seckill/search/upload/**comment（单数）**/admin + **sendmsg（`/api/sms/**`、`/api/message/**`）**。

**新增服务路由**：`gateway-dev.yml` 加 route（uri 用 `lb://{服务名}`）+ 如需免登录再加白名单。

# unimall-gateway 网关模块说明

> 微服务统一入口：服务路由（Nacos 负载均衡）、JWT 鉴权（Redis 白名单）、CORS 跨域。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、路由设计](#三路由设计)
- [四、鉴权设计](#四鉴权设计)
- [五、CORS 设计](#五cors-设计)
- [六、配置说明](#六配置说明)
- [七、目录结构](#七目录结构)
- [八、启动前提](#八启动前提)

---

## 一、模块定位

`unimall-gateway` 是集群的**统一入口**（端口 **10011**，Spring Cloud Gateway，WebFlux 响应式）：

- 前端只访问网关，网关按服务名经 Nacos 负载均衡转发
- 网关统一处理 **JWT 鉴权**（Redis 白名单）与 **CORS 跨域**
- 路由 / CORS / 白名单配置全部在配置中心 `gateway-dev.yml` 管理

## 二、技术栈

| 组件 | 说明 |
|---|---|
| `spring-cloud-starter-gateway` | Spring Cloud Gateway（**WebFlux，禁止引入 spring-boot-starter-web**） |
| `spring-cloud-starter-alibaba-nacos-discovery` | 服务发现（`lb://` 路由） |
| `spring-cloud-starter-loadbalancer` | 负载均衡（2023 系无 Ribbon，必须引入） |
| `spring-cloud-starter-config` | 配置中心客户端 |
| `spring-boot-starter-data-redis-reactive` | WebFlux 下操作 Redis（token 白名单） |
| `unimall-common` | `JwtUtil` / `Result` |

## 三、路由设计

统一前缀 `/api/{服务}/**` + `StripPrefix=1`（`/api/user/login` → 服务端 `/user/login`）。

| id | 路径 | 目标 |
|---|---|---|
| unimall-service-user | `/api/user/**` | `lb://unimall-service-user` |
| unimall-service-goods | `/api/goods/**` | `lb://unimall-service-goods` |
| unimall-service-order | `/api/order/**` | `lb://unimall-service-order` |
| unimall-service-cart | `/api/cart/**` | `lb://unimall-service-cart` |
| unimall-service-seckill | `/api/seckill/**` | `lb://unimall-service-seckill` |
| unimall-service-search | `/api/search/**` | `lb://unimall-service-search` |
| unimall-service-upload | `/api/upload/**` | `lb://unimall-service-upload` |
| unimall-service-comments | `/api/comments/**` | `lb://unimall-service-comments` |
| unimall-service-admin | `/api/admin/**` | `lb://unimall-service-admin` |

> 路由在配置中心 `gateway-dev.yml`（`spring.cloud.gateway.routes`），改路由无需动代码。`sendmsg` 未配 lb 路由（`/api/sms/send` 在白名单但无路由，WIP）。

## 四、鉴权设计

### Redis 白名单模式

```
登录成功(user) → JWT(jti) → Redis SET login:token:{jti}=userId (TTL 1800s)
网关校验 → 解析 JWT → GET login:token:{jti} → 存在=有效放行 / 不存在=401
登出 → DEL login:token:{jti}
```

### `AuthGlobalFilter`（GlobalFilter + Ordered，order=-100）

1. **白名单匹配**（`unimall.gateway.auth.whitelist`，`startsWith` 匹配）→ 放行
2. 取 `Authorization: Bearer <token>`，无/前缀错 → 401
3. `JwtUtil.parseToken` 失败（过期/篡改）→ 401
4. `GET login:token:{jti}` 不存在 → 401
5. 通过：附加 `X-User-Id` 头转发（服务端信任该头，不再重复验签）

401 响应：`Result.fail(401, "未登录或token已失效")`，JSON。

### 白名单清单（`gateway-dev.yml`）

```
/api/user/login、/api/user/register
/api/goods/list、/api/goods/detail、/api/category/list
/api/comment/list
/api/upload/            ← 带尾斜杠：只放行资源路径，上传接口仍需登录
/api/sms/send
/api/search/goods
/api/admin              ← 管理面整体放行（鉴权在 admin 内部拦截器）
```

### 密钥管理

`unimall.jwt.secret` 来自配置中心共享 `config-repo/application.yml`（user 签发与网关校验同一密钥），生产用环境变量 `JWT_SECRET` 注入。

## 五、CORS 设计

配置中心 `gateway-dev.yml` 全局 CORS：

- `add-to-simple-url-handler-mapping: true`（处理预检 OPTIONS，缺失会 404）
- `allowedOrigins: http://localhost:5173`；`allowedHeaders: "*"`（放行 Authorization）
- `allowCredentials: true` + 具体 origin（credentials 模式禁止通配符）

> CORS 只在网关配一次，下游服务一律不配。

## 六、配置说明

### 本地 `application.yml`（仅引导配置）

```yaml
server:
  port: 10011
spring:
  application:
    name: unimall-gateway
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    config:
      name: gateway
      profile: dev
  config:
    import: configserver:http://127.0.0.1:10010
```

### 配置中心 `gateway-dev.yml`（业务配置）

路由 / CORS / Redis（虚拟机 192.168.89.101）/ 白名单。

## 七、目录结构

```
unimall-gateway/src/main/java/com/unimall/gateway/
├── GatewayApplication.java
├── config/AuthConfig.java          # JwtUtil Bean
├── config/AuthProperties.java      # 白名单绑定
└── filter/AuthGlobalFilter.java    # 全局鉴权过滤器（-100）
```

## 八、启动前提

1. Nacos 启动（服务发现）
2. `unimall-config` 启动（native 激活后提供配置）——**当前未激活，网关启动会失败**
3. Redis 虚拟机（鉴权查白名单）
4. 业务服务注册到 Nacos（否则对应路由 503）

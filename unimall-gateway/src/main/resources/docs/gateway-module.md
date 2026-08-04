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

`unimall-gateway` 是集群的**统一入口**（Spring Cloud Gateway，WebFlux 响应式）：

- 前端只访问网关，由网关按服务名经 Nacos 做负载均衡转发
- 网关统一处理 **JWT 鉴权**（Redis 白名单模式）与 **CORS 跨域**
- 配置从 `unimall-config` 拉取（路由、CORS、鉴权白名单全部在配置中心管理）

## 二、技术栈

| 组件 | 说明 |
|---|---|
| `spring-cloud-starter-gateway` | Spring Cloud Gateway（**WebFlux，禁止引入 spring-boot-starter-web**） |
| `spring-cloud-starter-alibaba-nacos-discovery` | 服务发现（`lb://` 路由） |
| `spring-cloud-starter-loadbalancer` | 负载均衡（Spring Cloud 2023 已移除 Ribbon，必须引入） |
| `spring-cloud-starter-config` | 配置中心客户端 |
| `spring-boot-starter-data-redis-reactive` | WebFlux 下操作 Redis（token 白名单） |
| `unimall-common` | `JwtUtil` / `Result` |
| jjwt 0.12.6 | JWT 解析校验 |

## 三、路由设计

统一前缀 `/api/{服务}/**`，`StripPrefix=1` 去掉 `/api` 后按服务名转发（如 `/api/user/login` → 服务端 `/user/login`）。

| id | 路径 | 目标服务 |
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

> 路由配置在配置中心 `gateway-dev.yml`（`spring.cloud.gateway.routes`），改路由无需动代码。

## 四、鉴权设计

### Redis 白名单模式

```
登录成功(user服务) → JWT(带 jti) → Redis SET login:token:{jti}=userId (TTL 1800s)
网关校验         → 解析 JWT → GET login:token:{jti} → 存在=有效放行 / 不存在=401
登出(user服务)    → DEL login:token:{jti}（主动失效）
```

### `AuthGlobalFilter`（GlobalFilter + Ordered，order=-100）

执行顺序：

1. **白名单匹配**：路径命中 `unimall.gateway.auth.whitelist`（如 `/api/user/login`、`/api/user/register`）→ 直接放行
2. **取 token**：`Authorization: Bearer <token>`，无头/前缀不对 → 401
3. **验签解析**：`JwtUtil.parseToken` 失败（过期/篡改）→ 401
4. **Redis 校验**：`GET login:token:{jti}` 不存在 → 401
5. **放行**：向转发请求附加 `X-User-Id` 头（值为 userId），服务端从该头取身份，不再重复验签

401 响应：`Result.fail(401, "未登录或token已失效")`，JSON + `application/json;charset=UTF-8`。

### 密钥管理

- `unimall.jwt.secret` 来自配置中心共享配置 `config-repo/application.yml`（user 签发与网关校验同一密钥）
- 生产用环境变量 `JWT_SECRET` 注入，禁止提交真实密钥

## 五、CORS 设计

配置中心 `gateway-dev.yml` 全局 CORS（`spring.cloud.gateway.globalcors`）：

- `add-to-simple-url-handler-mapping: true`：处理浏览器预检 `OPTIONS` 请求（缺失会导致预检 404）
- `allowedOrigins: http://localhost:5173`（前端开发地址）
- `allowedHeaders: "*"`：放行 `Authorization`（JWT header 携带方式）
- `allowCredentials: true` + 具体 origin（浏览器规范：credentials 模式禁止通配符 origin）

> CORS 只在网关配一次，下游服务一律不配（服务端到服务端不受同源策略约束）。

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

- `spring.cloud.gateway.routes`：9 条路由
- `spring.cloud.gateway.globalcors`：CORS
- `spring.data.redis`：虚拟机 `192.168.89.101:6379`
- `unimall.gateway.auth.whitelist`：登录/注册白名单

## 七、目录结构

```
unimall-gateway/src/main/java/com/unimall/gateway/
├── GatewayApplication.java         # @SpringBootApplication 启动类
├── config/
│   ├── AuthConfig.java             # JwtUtil Bean（读 unimall.jwt.*）
│   └── AuthProperties.java         # 白名单绑定（unimall.gateway.auth.*）
└── filter/
    └── AuthGlobalFilter.java       # 全局鉴权过滤器（-100）

unimall-gateway/src/main/resources/
└── application.yml                 # 引导配置（端口 10011 / nacos / config）
```

## 八、启动前提

1. Nacos 启动（127.0.0.1:8848）——服务发现与 `lb://` 路由依赖
2. `unimall-config` 启动（native 模式提供 `gateway-dev.yml` / `application.yml`）——**当前未激活，网关启动会拉取失败**
3. Redis 启动（192.168.89.101:6379）——鉴权过滤器查询白名单依赖
4. 目标业务服务注册到 Nacos（如 unimall-service-user），否则对应路由返回 503

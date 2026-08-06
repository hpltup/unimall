# unimall-config — 配置中心

Spring Cloud Config **Server**（端口 **10010**，`@EnableConfigServer`）。集群所有服务的配置来源。

## 配置仓库 `src/main/resources/config-repo/`

| 文件 | 归属 | 说明 |
|---|---|---|
| `application.yml` | 所有服务共享 | `unimall.jwt.secret` / `expire-seconds`（user 签发 + gateway 校验同一密钥） |
| `gateway-dev.yml` | gateway | 路由 / CORS / 白名单 |
| `user-dev.yml` | user | 数据源 / Redis / MyBatis-Plus |
| `registry-dev.yml` | registry | 占位配置（registry 无数据源/Redis 依赖） |

**新增服务时**：在 config-repo 创建 `{spring.cloud.config.name}-dev.yml`（name 见各服务 `application.yml` 的 `spring.cloud.config.name`）。

## 关键实现

- `filter/MyFilter.java`：拦截 `/refresh-config-bus`，用 `MyRequestWrapper` 包空流放行（防止其他拦截器读取请求体）
- `utils/MyRequestWrapper.java`：重写 `getInputStream()`/`getReader()` 返回空流
- `myconfig/MvcConfig.java`：注册 MyFilter，`/*` + 最高优先级

## 已知问题（WIP）

1. **config 采用 gitee git 仓库模式**（`spring.cloud.config.server.git` + `search-paths: '*-dev'`），非 native——配置文件在 gitee 仓库 `unimall-config-dev`（13 个）
2. **config 自身配置在本地 application.yml**：config 是 Server 不拉 gitee 共享配置，`management`/`rabbitmq` 必须写本地（busrefresh 端点依赖）
3. **Bus 已接入**：`spring-cloud-starter-bus-amqp` + actuator，`POST /actuator/busrefresh` 广播热刷新已验证（RabbitMQ 虚拟机 `user/123456`）

## 注意

- 凭据（DB/Redis 密码）目前明文在 config-repo 中，仅限本地开发
- 共享配置 `application.yml` 优先级低于 `{name}-dev.yml`，各服务配置不要覆盖共享项（如 JWT 密钥）

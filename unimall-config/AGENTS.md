# unimall-config — 配置中心

Spring Cloud Config **Server**（端口 **10010**，`@EnableConfigServer`）。集群所有服务的配置来源。

## 配置仓库 `src/main/resources/config-repo/`

| 文件 | 归属 | 说明 |
|---|---|---|
| `application.yml` | 所有服务共享 | `unimall.jwt.secret` / `expire-seconds`（user 签发 + gateway 校验同一密钥） |
| `gateway-dev.yml` | gateway | 路由 / CORS / 白名单 |
| `user-dev.yml` | user | 数据源 / Redis / MyBatis-Plus |
| `registry-dev.yml` | registry | **缺失，需补齐** |

**新增服务时**：在 config-repo 创建 `{spring.cloud.config.name}-dev.yml`（name 见各服务 `application.yml` 的 `spring.cloud.config.name`）。

## 关键实现

- `filter/MyFilter.java`：拦截 `/refresh-config-bus`，用 `MyRequestWrapper` 包空流放行（防止其他拦截器读取请求体）
- `utils/MyRequestWrapper.java`：重写 `getInputStream()`/`getReader()` 返回空流
- `myconfig/MvcConfig.java`：注册 MyFilter，`/*` + 最高优先级

## 已知问题（WIP）

1. **native 模式未激活**：`application.yml` 未配 `spring.profiles.active=native` + `search-locations`，`config-repo/` 当前不会被任何服务拉到（各服务启动拉配置会失败）
2. `registry-dev.yml` 缺失
3. `/refresh-config-bus` 已就绪但 Spring Cloud Bus（RabbitMQ）未接入

## 注意

- 凭据（DB/Redis 密码）目前明文在 config-repo 中，仅限本地开发
- 共享配置 `application.yml` 优先级低于 `{name}-dev.yml`，各服务配置不要覆盖共享项（如 JWT 密钥）

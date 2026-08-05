# unimall-config 配置中心模块说明

> Spring Cloud Config Server：为集群所有服务提供统一配置管理（`config-repo` 本地仓库），并预留总线刷新端点。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、配置仓库（config-repo）](#三配置仓库config-repo)
- [四、关键实现](#四关键实现)
- [五、配置说明](#五配置说明)
- [六、目录结构](#六目录结构)
- [七、已知问题与下一步](#七已知问题与下一步)

---

## 一、模块定位

`unimall-config` 是集群的**配置中心**（Spring Cloud Config **Server**，端口 **10010**）：

- 所有服务通过 `spring.config.import: configserver:http://127.0.0.1:10010` 拉取配置
- 配置来源：本地 `config-repo/` 目录（native 模式，**尚未启用**，见"已知问题"），后续可切换远程 git 仓库
- 预留 `/refresh-config-bus` 总线刷新端点（Spring Cloud Bus / RabbitMQ，尚未接入）

> 注册中心是 Nacos（`unimall-registry`），配置中心是本模块——Spring Cloud Config + Nacos 注册的混合架构。

## 二、技术栈

| 组件 | 说明 |
|---|---|
| `spring-cloud-config-server` | Config Server 能力（`@EnableConfigServer`） |
| `spring-boot-starter-web` | Web 服务 |
| Spring Cloud Bus（计划） | 配置动态刷新广播（RabbitMQ，未接入） |

## 三、配置仓库（config-repo）

`src/main/resources/config-repo/` 存放所有服务的配置文件（**13 个，已齐全**）：

| 文件 | 归属 | 内容 |
|---|---|---|
| `application.yml` | **所有服务共享** | `unimall.jwt.secret` / `expire-seconds`（user 签发 + gateway/admin 校验同一密钥） |
| `user-dev.yml` / `goods-dev.yml` / `cart-dev.yml` / `order-dev.yml` / `seckill-dev.yml` / `comments-dev.yml` / `upload-dev.yml` / `sendmsg-dev.yml` / `search-dev.yml` / `admin-dev.yml` / `gateway-dev.yml` | 各业务服务 | 数据源、Redis、路由、白名单等 |
| `registry-dev.yml` | unimall-registry | 占位配置（registry 无数据源/Redis 依赖） |

**加载规则**：每个服务启动时拉取 `application.yml`（共享）+ `{spring.cloud.config.name}-{profile}.yml`（如 `gateway-dev.yml`），后者优先级更高——**各服务不要覆盖共享配置**（如 JWT 密钥）。

## 四、关键实现

### 1. `filter/MyFilter.java` — 总线刷新请求过滤器

- 拦截 URI 以 `/refresh-config-bus` 结尾的请求，用 `MyRequestWrapper` 包一层后放行，**使请求体变为空流**
- 目的：总线刷新端点无需请求体；防止其他拦截器读取/消费该流

### 2. `utils/MyRequestWrapper.java` — 空流请求包装器

- 重写 `getInputStream()` → 空 `ServletInputStream`（`read()` 返回 -1）
- 重写 `getReader()` → 空 `BufferedReader`（避免 `IllegalStateException`）

### 3. `myconfig/MvcConfig.java` — 过滤器注册

- `FilterRegistrationBean<MyFilter>`，`/*` + `Ordered.HIGHEST_PRECEDENCE`

### 4. `UniMallApplication.java` — 启动类

`@SpringBootApplication` + `@EnableConfigServer`。

## 五、配置说明

`application.yml`：

```yaml
server:
  port: 10010
spring:
  application:
    name: unimall-config
```

（激活 native 模式需追加 `spring.profiles.active=native` + `spring.cloud.config.server.native.search-locations: classpath:/config-repo`，见第七节。）

## 六、目录结构

```
unimall-config/src/main/java/com/unimall/config/
├── UniMallApplication.java          # @EnableConfigServer 启动类
├── filter/MyFilter.java             # /refresh-config-bus 过滤器
├── utils/MyRequestWrapper.java      # 空流请求包装
└── myconfig/MvcConfig.java          # 过滤器注册

unimall-config/src/main/resources/
├── application.yml                  # 端口 10010
└── config-repo/                     # 配置仓库（13 个文件，待 native 激活）
```

## 七、已知问题与下一步

1. **native 模式未激活（关键阻塞）**：`application.yml` 未配置 `spring.profiles.active=native` + search-locations，`config-repo/` 下所有文件当前**不会被任何服务拉到**（各服务启动拉配置会失败）
2. **Bus / RabbitMQ 未接入**：`/refresh-config-bus` 端点与过滤器已就绪，但消息总线未装，动态刷新链路未通

**下一步**：激活 native 模式（或用远程 git 仓库）、接入 Bus。

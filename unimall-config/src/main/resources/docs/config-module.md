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

`unimall-config` 是集群的**配置中心**（Spring Cloud Config **Server**）：

- 各服务（gateway / user / registry）通过 `spring.config.import: configserver:http://127.0.0.1:10010` 拉取配置
- 配置来源：本地 `config-repo/` 目录（native 模式，尚未启用，见"已知问题"），后续可切换远程 git 仓库
- 预留 `/refresh-config-bus` 总线刷新端点：接入 Spring Cloud Bus（RabbitMQ）后可广播配置变更，支持动态刷新

> 注意：集群的注册中心是 Nacos（`unimall-registry`），配置中心是本模块（Spring Cloud Config）——两者分离，属 Spring Cloud Config + Nacos 注册的混合架构。

## 二、技术栈

| 组件 | 说明 |
|---|---|
| `spring-cloud-config-server` | Config Server 能力（`@EnableConfigServer`） |
| `spring-boot-starter-web` | Web 服务 |
| Spring Cloud Bus（计划） | 配置动态刷新广播（RabbitMQ，尚未接入） |

## 三、配置仓库（config-repo）

`src/main/resources/config-repo/` 存放各服务的配置文件（计划由 native 模式提供，**尚未激活**）：

| 文件 | 归属服务 | 内容 |
|---|---|---|
| `application.yml` | **所有服务共享** | `unimall.jwt.secret` / `expire-seconds`（user 签发 + gateway 校验同一密钥） |
| `gateway-dev.yml` | unimall-gateway | 9 条路由、CORS、`unimall.gateway.auth.whitelist` |
| `user-dev.yml` | unimall-service-user | 数据源、Redis、MyBatis-Plus 配置 |
| `registry-dev.yml` | unimall-registry | **尚未创建**（registry 启动拉取会 404，需补齐） |

**加载规则**（Spring Cloud Config 客户端）：每个服务启动时拉取 `application.yml`（共享）+ `{spring.cloud.config.name}-{profile}.yml`（如 `gateway-dev.yml`），后者优先级更高。

## 四、关键实现

### 1. `filter/MyFilter.java` — 总线刷新请求过滤器

- 拦截 URI 以 `/refresh-config-bus` 结尾的请求
- 用 `MyRequestWrapper` 包一层后放行，**使请求体变为空流**
- 目的：总线刷新端点无需请求体；防止其他拦截器读取/消费该流导致问题

### 2. `utils/MyRequestWrapper.java` — 空流请求包装器

- 重写 `getInputStream()` → 返回空 `ServletInputStream`（`read()` 立即返回 -1）
- 重写 `getReader()` → 返回空 `BufferedReader`（避免调用报 `IllegalStateException`）

### 3. `myconfig/MvcConfig.java` — 过滤器注册

- `@Configuration` + `FilterRegistrationBean<MyFilter>`
- 拦截 `/*`，`Ordered.HIGHEST_PRECEDENCE` 最高优先级，保证先于其他过滤器替换请求流

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

服务端口 **10010**，各服务的 `configserver` 地址统一指向它。

## 六、目录结构

```
unimall-config/src/main/java/com/unimall/config/
├── UniMallApplication.java          # @EnableConfigServer 启动类
├── filter/MyFilter.java             # /refresh-config-bus 过滤器
├── utils/MyRequestWrapper.java      # 空流请求包装
└── myconfig/MvcConfig.java          # 过滤器注册

unimall-config/src/main/resources/
├── application.yml                  # 端口 10010
└── config-repo/                     # 配置仓库（待 native 激活）
    ├── application.yml
    ├── gateway-dev.yml
    └── user-dev.yml
```

## 七、已知问题与下一步

1. **native 模式未激活**：`application.yml` 尚未配置 `spring.profiles.active=native` + `spring.cloud.config.server.native.search-locations`，`config-repo/` 下的文件当前**不会被任何服务拉到**（各服务启动拉配置会失败）
2. **`registry-dev.yml` 缺失**：registry 服务配置的 `name=registry`，仓库里没有该文件
3. **Bus / RabbitMQ 未接入**：`/refresh-config-bus` 端点和过滤器已就绪，但消息总线（RabbitMQ）未安装/配置，动态刷新链路未通

**下一步**：激活 native 模式（或用远程 git 仓库）、补齐 `registry-dev.yml`、接入 Bus。

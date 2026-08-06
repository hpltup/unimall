# unimall-config 配置中心模块说明

> Spring Cloud Config Server：**gitee git 仓库模式**为集群提供统一配置管理，已接入 Bus 动态刷新，并预留 `/refresh-config-bus` 端点。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、配置仓库（gitee unimall-config-dev）](#三配置仓库gitee-unimall-config-dev)
- [四、关键实现](#四关键实现)
- [五、配置说明](#五配置说明)
- [六、目录结构](#六目录结构)
- [七、已知问题与下一步](#七已知问题与下一步)

---

## 一、模块定位

`unimall-config` 是集群的**配置中心**（Spring Cloud Config **Server**，端口 **10010**）：

- 所有服务通过 `spring.config.import: configserver:http://127.0.0.1:10010` 拉取配置
- 配置来源：**gitee 私有仓库 `unimall-config-dev`**（git 模式，13 个配置文件）
- **Bus 已接入**：`POST /actuator/busrefresh` 广播刷新，改配置无需重启服务（已验证）

## 二、技术栈

| 组件 | 说明 |
|---|---|
| `spring-cloud-config-server` | Config Server 能力（`@EnableConfigServer`） |
| `spring-cloud-starter-bus-amqp` | Bus 动态刷新广播（RabbitMQ，虚拟机 `user/123456`） |
| `spring-boot-starter-actuator` | 暴露 `/actuator/busrefresh` 等端点 |
| `spring-boot-starter-web` | Web 服务 |

## 三、配置仓库（gitee unimall-config-dev）

配置在 **gitee 仓库 `unimall-config-dev`**（Config Server git 模式，`search-paths: '*-dev'`）：

| 文件 | 归属 | 内容 |
|---|---|---|
| `application.yml`（根目录） | **所有服务共享** | `unimall.jwt.secret` / `expire-seconds`、**RabbitMQ 连接、actuator 端点暴露** |
| `{name}-dev/{name}-dev.yml`（子目录） | 各业务服务 | 数据源、Redis、路由、白名单等（`search-paths: '*-dev'` 让 Server 进子目录找） |

**加载规则**：每个服务启动时拉取 `application.yml`（共享）+ `{spring.cloud.config.name}-{profile}.yml`（如 `gateway-dev.yml`），后者优先级更高——**各服务不要覆盖共享配置**（如 JWT 密钥、RabbitMQ）。

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

`application.yml`（**config 本地**——config 是 Server 不拉 gitee 共享配置，故 git/rabbitmq/management 都写本地）：

```yaml
server:
  port: 10010
spring:
  application:
    name: unimall-config
  rabbitmq:                      # Bus 连接（必须本地配置）
    host: 192.168.89.101
    port: 5672
    username: user
    password: 123456
  cloud:
    config:
      server:
        git:
          uri: https://gitee.com/mystic-voyage/unimall-config-dev.git
          username: mystic-voyage
          password: ${GITEE_TOKEN}   # 私人令牌，环境变量注入
          default-label: master
          search-paths: '*-dev'      # 业务文件在 {name}-dev/ 子目录
management:
  endpoints:
    web:
      exposure:
        include: busrefresh,refresh,health
```

## 六、目录结构

```
unimall-config/src/main/java/com/unimall/config/
├── UniMallApplication.java          # @EnableConfigServer 启动类
├── filter/MyFilter.java             # /refresh-config-bus 过滤器
├── utils/MyRequestWrapper.java      # 空流请求包装
└── myconfig/MvcConfig.java          # 过滤器注册

unimall-config/src/main/resources/
└── application.yml                  # 端口/git/rabbitmq/management（本地配置）
```

> 项目内 `config-repo/` 目录保留为本地同步副本（git 模式下不被使用）。

## 七、已知问题与下一步

1. **config 自身配置在本地**：config 不拉 gitee 共享配置，`management`/`rabbitmq` 必须写本地 `application.yml`（曾漏配导致 `/actuator/busrefresh` 不可用）
2. **`/refresh-config-bus` 为预留端点**：标准 Bus 刷新走 `/actuator/busrefresh`（已验证），该自定义端点/过滤器保留待用

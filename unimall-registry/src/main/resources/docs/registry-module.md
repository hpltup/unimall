# unimall-registry 注册中心模块说明

> 服务注册与发现基础设施：Nacos Server（独立部署）+ 本模块作为配置客户端管理应用。

## 目录

- [一、模块定位](#一模块定位)
- [二、部署方式](#二部署方式)
- [三、技术栈](#三技术栈)
- [四、配置说明](#四配置说明)
- [五、目录结构](#五目录结构)
- [六、已知问题](#六已知问题)

---

## 一、模块定位

`unimall-registry` = 集群的**服务注册与发现基础设施**：

- **注册中心本体**：Nacos Server（独立中间件，本地 `nacos-server-2.3.2` 发行包部署）
- **本模块**：`RegistryApplication` 作为配置中心客户端，从 `unimall-config` 拉取自身配置（`registry-dev.yml`），是"注册中心服务也是配置中心客户端"的承载

所有业务服务（gateway / user / goods / order ...）启动后向 Nacos 注册，网关通过 Nacos 做服务路由与负载均衡。

## 二、部署方式

**Nacos Server 独立部署**（官方支持方式，不内嵌 Spring Boot）：

```
nacos-server-2.3.2/
└── bin/
    ├── startup.cmd -m standalone   # Windows 单机启动（内置 Derby 存储）
    └── shutdown.cmd                # 停止
```

- 版本：**2.3.2**（与项目 spring-cloud-alibaba 2023.0.1.0 的 Nacos 客户端 2.3.x 同版本族）
- 端口：8848（HTTP）/ 9848（gRPC），控制台 `http://127.0.0.1:8848/nacos`
- 环境要求：JDK 17+（本项目 Java 21 满足）

## 三、技术栈

| 组件 | 说明 |
|---|---|
| `spring-boot-starter-web` | 管理应用 Web 能力 |
| `spring-cloud-starter-config` | 配置中心客户端（从 unimall-config 拉配置） |
| Nacos Server 2.3.2 | 注册中心本体（独立进程） |

## 四、配置说明

`application.yml`：

```yaml
spring:
  application:
    name: unimall-registry
  cloud:
    config:
      name: registry
      profile: dev
  config:
    import: configserver:http://127.0.0.1:10010
```

- 从 `unimall-config` 拉取 `registry-dev.yml`
- **未配置 `server.port`** → 默认 8080（注意与 gateway 10011 / user 10012 / config 10010 区分）
- 采用 Spring Cloud 2023 推荐写法 `spring.config.import`（非 bootstrap.yml）

## 五、目录结构

```
unimall-registry/src/main/java/com/unimall/registry/
└── RegistryApplication.java        # @SpringBootApplication 启动类

unimall-registry/src/main/resources/
└── application.yml                 # 配置中心客户端引导配置
```

## 六、已知问题

1. **`registry-dev.yml` 尚未在配置仓库提供**：`unimall-config/config-repo/` 下暂无该文件，registry 启动拉取配置会 404 失败，需在 config 端补齐
2. **config 端 native 模式未激活**：与上一条联动，config-repo 整体尚未被任何服务拉到（见 unimall-config 文档"已知问题"）
3. **Nacos 需独立启动**：开发/运行前需在 `nacos-server-2.3.2` 目录执行 `startup.cmd -m standalone`

**下一步**：config 端激活 native + 补齐 `registry-dev.yml`，启动 Nacos 后即可验证注册中心链路。

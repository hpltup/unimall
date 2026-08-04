# unimall-registry — 注册中心

集群服务注册与发现基础设施。**注册中心本体 = Nacos Server 独立部署**（官方方式，不内嵌 Spring Boot）；本模块仅是一个配置客户端壳应用。

## Nacos Server（本地 2.3.2 发行包，独立进程）

- 启动：`nacos-server-2.3.2/bin/startup.cmd -m standalone`（单机 + 内置 Derby 存储）
- 端口：8848（HTTP）/ 9848（gRPC），控制台 `http://127.0.0.1:8848/nacos`
- 版本 2.3.2 与 spring-cloud-alibaba 2023.0.1.0 的客户端 2.3.x 同版本族
- 各服务 `spring.cloud.nacos.discovery.server-addr: 127.0.0.1:8848` 注册到它

## 本模块（RegistryApplication）

- `application.yml`：`spring.cloud.config.name: registry`，`config.import: configserver:http://127.0.0.1:10010`，从 unimall-config 拉 `registry-dev.yml`
- **未配 `server.port`**（默认 8080），与 config 10010 / gateway 10011 / user 10012 区分
- 采用 Spring Cloud 2023 的 `spring.config.import` 写法（不用 bootstrap.yml）

## 已知问题

- `config-repo/registry-dev.yml` 缺失 → registry 启动拉配置 404，需在 unimall-config 补齐
- 运行前需手动启动 Nacos（本模块不内嵌）

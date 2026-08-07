# AGENTS.md — UniMall 微服务商城

本文件供 AI 编码代理（及新加入的开发者）快速理解本项目。项目文档与代码注释均为**中文**，本文件亦使用中文撰写。

## 一、项目概述

UniMall 是一个基于 Spring Cloud Alibaba 的**微服务商城系统**（毕业设计/实习项目，开发进行中）。

- 统一入口：`unimall-gateway`（Spring Cloud Gateway，WebFlux）
- 注册中心：`unimall-registry`（Nacos Server 独立部署 + 一个配置客户端壳模块）
- 配置中心：`unimall-config`（Spring Cloud Config Server，native 仓库模式）
- 业务服务：目前仅 `unimall-service-user`（用户服务）实现完成，其余 10 个模块为**空骨架**（仅有 pom.xml，待开发）

### 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | `maven.compiler.source/target=21` |
| Spring Boot | 3.3.7 | 根 pom 作为 parent |
| Spring Cloud | 2023.0.1 | 2023.0 系已移除 Ribbon，需显式引入 loadbalancer |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos discovery |
| Nacos | 2.3.2（独立部署） | 注册中心，8848(HTTP)/9848(gRPC)，`startup.cmd -m standalone` 单机启动 |
| MySQL | 8.0.33 | 驱动 `mysql-connector-j` |
| MyBatis-Plus | 3.5.5 | `mybatis-plus-spring-boot3-starter` + `mybatis-plus-extension`（后者需显式引入） |
| Redis | - | 登录 token 白名单；网关用 reactive 版，MVC 服务用普通版 |
| jjwt | 0.12.6 | JWT，版本在 `unimall-common/pom.xml` 写死（根 pom 未管理） |
| Lombok | 1.18.30 | `@Data` 等 |
| Spring Cloud Bus (AMQP) | 2023.0.1 管理 | 配置动态刷新广播（RabbitMQ，虚拟机 `user/123456`），`/actuator/busrefresh` |
| Actuator | Boot 3.3 管理 | 所有服务暴露 `busrefresh,refresh,health` 端点 |
| Spring AI | 1.0.0（Maven Central 正式版，根 pom `spring-ai-bom` 管理） | AI 客服模块用：`spring-ai-starter-model-openai` 走 OpenAI 兼容协议接 DeepSeek，`@Tool` 函数调用 + SSE 流式 |

## 二、模块划分

根 pom（`groupId=com.unimall`, `artifactId=UniMall`, `version=1.0-SNAPSHOT`, packaging=pom）聚合 15 个模块。

### 已实现（15 个）

| 模块 | 端口 | 职责 |
|---|---|---|
| `unimall-common` | -（纯 Java 库） | 公共组件：`Result`、`BusinessException`、`JwtUtil`、跨服务共享 VO（`GoodsVO`）。**零 Spring 依赖**（保证网关 WebFlux 与业务 MVC 共用不污染类路径），无 Lombok，POJO 手写 getter/setter |
| `unimall-config` | 10010 | Spring Cloud Config Server（`@EnableConfigServer`）；`config-repo/` 本地仓库；预留 `/refresh-config-bus` 总线刷新端点 |
| `unimall-gateway` | 10011 | 统一入口：9 条路由（`lb://` + Nacos 负载均衡）、JWT 鉴权过滤器、CORS |
| `unimall-registry` | 8080（默认，未配 `server.port`） | Nacos Server 部署说明 + 配置客户端壳应用 |
| `unimall-service-user` | 10012 | 用户服务：注册/登录（签发 JWT + 写 Redis 白名单）/用户信息查询 |
| `unimall-service-goods` | 10013 | 商品服务（简化版建模：category + goods 单表）：分类/商品分页/详情/新增/上下架，含内部批量接口 `/goods/batch` |
| `unimall-service-cart` | 10014 | 购物车服务（OpenFeign）：添加（调 goods 校验）/列表（Feign 批量查商品）/改数量/删除；含内部接口 `internal/checked`、`internal/remove`（订单调用） |
| `unimall-service-order` | 10015 | 订单服务（OpenFeign 三服务链路）：下单（cart 选中条目 → goods 原子扣库存 → 建订单快照 → 清购物车）/订单分页/详情/模拟支付/取消（回库存） |
| `unimall-service-seckill` | 10016 | 秒杀服务（Redis Lua 防超卖）：活动管理/列表/抢购（Lua 原子扣库存+限购→同步建单）/结果查询（预留 MQ 演进） |
| `unimall-service-comments` | 10017 | 评论服务：发表评论（需登录）/按商品查评论（公开，白名单）/我的评论/删除自己的评论 |
| `unimall-service-upload` | 10018 | 文件上传（本地磁盘存储）：POST 上传（需登录，UUID 重命名+扩展名白名单）→ 返回 `/api/upload/{文件名}`；静态资源映射 `/upload/**` 公开访问 |
| `unimall-service-sendmsg` | 10019 | 消息服务：短信验证码（模拟发送，Redis TTL 5 分钟 + 一次性校验，`/api/sms/send` 白名单）+ 站内信（发送/列表/未读数/标记已读） |
| `unimall-service-search` | 10020 | 搜索服务（Elasticsearch 8.x + IK/拼音分词）：定时全量同步 goods → ES（10 分钟 + 手动 `POST /search/sync`）、`GET /search/goods` 全文检索（相关性 + 销量排序） |
| `unimall-service-admin` | 10021 | 后台管理（聚合层，Feign 调各服务）：管理员登录（独立 JWT + Redis `admin:token:` 白名单，内部拦截器鉴权）、商品/订单/用户/秒杀活动管理 |
| `unimall-service-ai` | 10022 | AI 客服（Spring AI 1.0.0 + DeepSeek，OpenAI 兼容协议）：SSE 流式对话 + Function Calling 工具（搜商品/加购/查购物车/确认后下单/查订单/取消订单），会话存 Redis；**支付不代付**（下单后引导用户在订单中心自行支付）；API Key 走 `DEEPSEEK_API_KEY` 环境变量 |

### 空骨架

**全部 15 个模块已实现**，无空骨架。

> `unimall-item` 已并入 `unimall-service-goods`（商品模型内聚在商品服务，根 pom 已移除该模块）。
> 网关路由统一 `/api/{服务}/**` + StripPrefix=1，全部业务服务已配路由（含 sendmsg 的 `/api/sms/**`、`/api/message/**` 与 ai 的 `/api/ai/**`）。

### 包名与代码分层约定

业务服务统一包名 `com.unimall.{服务名}`（如 `com.unimall.user`），分层结构：

```
com.unimall.user/
├── XxxApplication.java      # 启动类（@SpringBootApplication，user 模块另加 @MapperScan）
├── controller/              # 只做参数接收与 Result 包装，不写业务
├── service/
│   ├── IUserService.java    # 接口（命名 I 开头）
│   └── impl/UserServiceImpl.java  # 实现，继承 MyBatis-Plus 的 ServiceImpl
├── mapper/IUserMapper.java  # 继承 BaseMapper<T>（命名 I 开头）
├── pojo/
│   ├── entity/              # 数据库实体（@TableName/@TableId/@TableLogic + @Data）
│   ├── dto/                 # 入参（@Data + jakarta.validation 注解）
│   └── vo/                  # 出参（字段白名单，不返回敏感字段）
└── config/                  # 全局异常处理器等
```

## 三、架构与关键链路

### 配置加载（重要）

- 每个服务本地 `application.yml` 只放**引导配置**：端口、`spring.application.name`、Nacos 地址、`spring.cloud.config.name/profile`、`spring.config.import: configserver:http://127.0.0.1:10010`
- 业务配置（数据源、Redis、路由、JWT 密钥等）全部在 **gitee 仓库 `unimall-config-dev`**（Config Server git 模式，13 个配置文件）：
  - `application.yml`：所有服务共享（`unimall.jwt.secret` / `expire-seconds`、**RabbitMQ 连接、actuator 端点暴露**）
  - `{name}-dev.yml`：按服务命名（`user-dev.yml`、`gateway-dev.yml`），优先级高于共享文件；**业务文件在 `{name}-dev/` 子目录**，Config Server 配了 `search-paths: '*-dev'`
- config 服务自身配置（git 仓库、RabbitMQ、management 端点）在**本地 `application.yml`**——config 是 Server 不拉 gitee 共享配置，这点与业务服务不同
- **Bus 动态刷新**：所有服务已接入 `spring-cloud-starter-bus-amqp` + actuator；改配置 → push gitee → `POST /actuator/busrefresh`（任一直连端口）广播 → 全服务热刷新，零重启

### 鉴权链路（JWT + Redis 白名单）

```
登录成功(user) → JWT(jti) → Redis SET login:token:{jti}=userId (TTL=expire-seconds 1800s)
用户请求 → 网关 AuthGlobalFilter(order=-100)
  ├─ 白名单命中（unimall.gateway.auth.whitelist，如 /api/user/login、/api/user/register）→ 放行
  └─ 校验 Authorization: Bearer <token>
     → JwtUtil.parseToken 验签 → Redis GET login:token:{jti} 存在？
     → 放行并附加 X-User-Id 头 / 401
下游服务从请求头 X-User-Id 取用户身份，不再重复验签（信任内网转发）
```

- **管理/内部接口保护**：网关 `AdminProtectFilter(order=-200)` 拦截普通用户访问管理操作与内部接口（`POST /api/goods`、`PUT /api/goods/status`、`/api/*/internal/**`、`/api/goods/batch|deduct|restore`、`POST /api/seckill/activity`）→ 403；管理操作只能走 `/api/admin/*`（admin 服务 Feign 直连，不经网关）
- Redis key 前缀 `login:token:`（网关 `AuthGlobalFilter` 与 user 服务 `UserServiceImpl` 各有一份常量，注意保持一致）
- JWT payload：`sub`=userId、`jti`=UUID、`iat`、`exp`；HS256，密钥必须 >= 32 字节

### 路由与跨域

- 统一前缀 `/api/{服务}/**` + `StripPrefix=1`（`/api/user/login` → 服务端 `/user/login`），路由在 `gateway-dev.yml` 配置，改路由无需动代码
- CORS 只在网关配（`globalcors`，允许 `http://localhost:5173`，`add-to-simple-url-handler-mapping: true` 处理预检 OPTIONS），下游服务一律不配

## 四、构建与运行

### 构建

```bash
mvn compile          # 编译（本地已用 mvn -q -o compile 验证通过，依赖已缓存可离线）
mvn package          # 打包
mvn clean install    # 全量构建并安装到本地仓库（unimall-common 被各服务依赖，改了它需要先 install）
```

- 环境：JDK 21（`/d/JDK/jdk-21`）、Maven 3.9.11
- 项目无 `mvnw` wrapper（`.mvn/` 目录为空），直接用系统 `mvn`
- `-o` 离线编译可用（依赖已在本地仓库缓存）

### 启动前提与顺序

1. MySQL（127.0.0.1:3306）：执行 `unimall-service-user/src/main/resources/sql/user.sql` 建库建表
2. Nacos（127.0.0.1:8848）：`nacos-server-2.3.2/bin/startup.cmd -m standalone`
3. Redis（192.168.89.101:6379，密码 Redis123456）
4. `unimall-config`（10010）→ 业务服务（如 `unimall-service-user` 10012）→ `unimall-gateway`（10011）

> 注意：数据库/Redis 凭据（root/SQL123456、Redis123456）目前直接写在 `config-repo` 的 yml 中，仅限本地开发。

### 运行时中间件

| 中间件 | 地址 | 凭据 |
|---|---|---|
| MySQL | 127.0.0.1:3306/unimall | root / SQL123456 |
| Redis | 192.168.89.101:6379 | Redis123456（db 0） |
| Nacos | 127.0.0.1:8848 | - |

## 五、代码规范

- **大括号换行（Allman 风格）**、4 空格缩进、无分号省略——与现有代码保持一致
- **注释与文档用中文**；每个模块在 `src/main/resources/docs/{模块名}-module.md` 写模块说明（新模块应补齐）
- 控制器/服务/接口返回统一用 `com.unimall.common.result.Result<T>`：`Result.ok()` / `Result.ok(data)` / `Result.fail(code, message)`；`code=0` 成功
- 业务异常一律 `throw new BusinessException(1001, "中文提示")`，由各服务内 `GlobalExceptionHandler`（`@RestControllerAdvice`）统一捕获转 `Result`
- **错误码约定**（user 模块现行，新模块沿用风格）：`1001 用户名已存在` / `1002 用户不存在` / `1003 密码错误` / `1004 账号已被禁用` / `1005 参数校验失败` / `5000 系统异常`；鉴权失败 `401`
- 依赖注入用**构造器注入**（final 字段）
- 实体使用 MyBatis-Plus 注解：`@TableId(type=IdType.AUTO)`、`@TableLogic`（逻辑删除字段 `deleted`，`config-repo` 已配置 logic-delete）、时间字段走数据库默认值（`CURRENT_TIMESTAMP`）
- DTO/VO 用 Lombok `@Data` + jakarta.validation（`@NotBlank`/`@Size`/`@Pattern`，带中文 message）
- **网关模块（WebFlux）禁止引入 `spring-boot-starter-web`**；MVC 服务用 `StringRedisTemplate`，网关用 `ReactiveStringRedisTemplate`
- SQL 脚本放各服务 `src/main/resources/sql/`，建表脚本含设计要点注释、唯一索引、逻辑删除字段

## 六、测试

- 项目**当前没有任何测试**（无 `src/test` 目录、无测试依赖、无 CI 配置）
- 新增代码如需验证，暂无现成测试框架可依赖，可先 `mvn compile` 保证编译通过；引入测试框架前先与项目现状对齐

## 七、安全注意事项

- JWT 密钥：`unimall.jwt.secret` 配置中心默认值为占位符，**生产必须用环境变量 `JWT_SECRET` 注入**（`${JWT_SECRET:...}` 写法已支持），禁止提交真实密钥
- 密码 BCrypt 加密存储（仅引入 `spring-security-crypto`，不引入完整 security）；数据库唯一索引 + 代码查重双保险
- 实名认证 `id_card` 按脱敏存储
- 网关鉴权通过后附加 `X-User-Id`，下游服务信任该头——服务必须保证不经网关不可达（不应直接暴露给外部）
- 登出/拉黑 = 删除 Redis 白名单 key（当前登出接口未实现，已预留机制）

## 八、开发中的已知缺口（WIP 状态）

1. ~~config native 未激活~~ → **已改为 gitee git 仓库模式**（`spring.cloud.config.server.git` + `search-paths: '*-dev'`），配置文件在 gitee 仓库 `unimall-config-dev`
2. ~~Bus 未接入~~ → **已接入**（`spring-cloud-starter-bus-amqp` + actuator，`POST /actuator/busrefresh` 广播热刷新已验证，RabbitMQ 虚拟机 `user/123456`）
3. ~~C 端登出接口未实现~~ → **已实现**：`POST /user/logout`（删 Redis `login:token:{jti}`，token 立即失效，已验证）
4. ~~运行验证未做~~ → **全链路验证已完成**（注册→下单→支付/取消→秒杀防超卖→搜索→后台→Bus 热刷新，详见 `TESTING.md`，含测试期修复的 15 个问题）
5. ~~Token 续期机制未做~~ → **已实现滑动续期**：JWT 7 天兜底（`expire-seconds: 604800`）+ Redis 会话 30 分钟滑动（`session-seconds: 1800`），网关/admin 拦截器校验通过即续 TTL，用户活跃不掉线、30 分钟无操作需重登

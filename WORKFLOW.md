# UniMall 开发工作流记录

> 本项目从空骨架到 14 个微服务模块全部实现 + 全链路测试通过的工作流整理。
> 与 `AGENTS.md`（项目约定/架构）、`TESTING.md`（测试步骤/踩坑）互补——本文件侧重**开发与测试的完整过程**。

## 目录

- [一、项目概览](#一项目概览)
- [二、开发工作流（从 0 到完整）](#二开发工作流从-0-到完整)
- [三、关键设计决策](#三关键设计决策)
- [四、测试工作流](#四测试工作流)
- [五、测试期修复的问题（16 个坑）](#五测试期修复的问题16-个坑)
- [六、文档体系](#六文档体系)
- [七、项目状态与后续](#七项目状态与后续)

---

## 一、项目概览

**基于 Spring Cloud Alibaba 的微服务商城**（学习/实习项目），14 个模块：

| 模块 | 端口 | 职责 |
|---|---|---|
| unimall-common | - | 公共组件（Result/异常/JwtUtil/跨服务 DTO-VO），纯 Java 零 Spring 依赖 |
| unimall-config | 10010 | 配置中心（Config Server，gitee git 模式） |
| unimall-gateway | 10011 | 统一入口（路由/JWT 鉴权/CORS/权限保护/Bus） |
| unimall-registry | 8080 | Nacos Server 部署说明 + 配置客户端壳 |
| unimall-service-user | 10012 | 用户（注册/登录/登出/信息/滑动续期） |
| unimall-service-goods | 10013 | 商品（分类/CRUD/上下架/库存扣减） |
| unimall-service-cart | 10014 | 购物车（首个 OpenFeign 模块） |
| unimall-service-order | 10015 | 订单（三服务协作下单/支付/取消） |
| unimall-service-seckill | 10016 | 秒杀（Redis Lua 防超卖 + 库存联动） |
| unimall-service-comments | 10017 | 评论 |
| unimall-service-upload | 10018 | 文件上传（本地存储） |
| unimall-service-sendmsg | 10019 | 短信验证码 + 站内信 |
| unimall-service-search | 10020 | 搜索（ES IK+拼音） |
| unimall-service-admin | 10021 | 后台管理（聚合层，单管理员） |

**技术栈**：JDK 21 / Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Spring Cloud Alibaba 2023.0.1.0 / Nacos 2.3.1 / MyBatis-Plus 3.5.5 / MySQL 8 / Redis 8 / ES 8.19.17 / RabbitMQ 3 / OpenFeign / jjwt 0.12.6 / BCrypt。

## 二、开发工作流（从 0 到完整）

### 阶段划分

| 阶段 | 模块 | 内容 |
|---|---|---|
| 1. 基础架构 | common / config / gateway / registry | 公共组件、配置中心、统一入口、注册中心 |
| 2. 核心业务 | user / goods / cart / order | 用户、商品、购物车、下单主链路 |
| 3. 扩展业务 | seckill / comments / upload / sendmsg / search | 秒杀、评论、上传、消息、搜索 |
| 4. 管理面 | admin | 后台管理聚合 |

### 单个模块的标准开发模式

```
1. pom 依赖（web/validation/mybatis-plus+extension/mysql/nacos/config/common/lombok，按需加 redis/openfeign）
2. 建表 SQL（src/main/resources/sql/，含注释/唯一索引/逻辑删除）
3. 启动类（@SpringBootApplication + @MapperScan，Feign 模块加 @EnableFeignClients）
4. 实体/Mapper（@TableName/@TableId/@TableLogic，I 开头命名）
5. DTO（@Valid 校验）/ VO（字段白名单，不含敏感字段）
6. Service（I 开头接口 + impl 继承 ServiceImpl）
7. Controller（只做参数接收 + Result 包装）
8. 配置中心 yml（config-repo/{name}-dev.yml：datasource/redis/mp）
9. 网关路由/白名单（gateway-dev.yml，C 端查询进白名单，管理接口不暴露）
10. 文档（AGENTS.md + docs/{模块}-module.md）
11. 编译验证（mvn -pl {模块} -am compile）
```

### 跨服务开发要点

- **Feign 契约放 common**：跨服务 DTO/VO（GoodsVO/CartItemVO/OrderVO/UserVO/GoodsStockDTO 等）必须放 `unimall-common`，不能放业务模块（避免跨服务依赖业务 jar）
- **内部接口 `/internal/**`**：服务间直连调用（cart→goods、order→cart/goods、admin→各服务），不走网关
- **物理删除 vs 逻辑删除**：历史数据表（订单/商品/用户）用逻辑删除；临时数据表（购物车）用物理删除（避免唯一索引冲突）

## 三、关键设计决策

### 1. 配置中心（gitee git 模式）

- Config Server 用 **gitee 私有仓库**（`unimall-config-dev`）+ `search-paths: '*-dev'`（业务文件在 `{name}-dev/` 子目录）
- 共享 `application.yml`（JWT/RabbitMQ/端点暴露）优先级低于 `{name}-dev.yml`
- **config 自身配置（git/rabbitmq/management）在本地 application.yml**——config 是 Server 不拉 gitee 共享配置

### 2. 鉴权链路（JWT + Redis 白名单 + 滑动续期）

```
登录 → JWT(jti) + Redis login:token:{jti}=userId（TTL 30 分钟滑动）
网关 AuthGlobalFilter(-100)：白名单放行 / 验签 / 查 Redis / 续 TTL / 附加 X-User-Id
管理员：/api/admin 整体放行，admin 内部 AdminAuthInterceptor 自管
权限保护 AdminProtectFilter(-200)：普通用户访问管理/内部接口 → 403
```

- JWT 7 天兜底（expire-seconds），Redis 会话 30 分钟**滑动**（session-seconds）——用户活跃不掉线
- 登出 = 删 Redis key，token 立即失效

### 3. 秒杀（Redis Lua 防超卖 + 库存联动）

- 库存预热到 Redis，**Lua 原子扣减 + 限购校验**（并发不可能超卖）
- 建单成功后 **Feign 联动扣减 goods 库存**，失败回滚（回补 Redis + 删订单）
- 预留 MQ 异步演进（createSeckillOrder 独立 + result 轮询接口）

### 4. 搜索（ES IK + 拼音分词）

- 索引自定义 analyzer：`ik_max_word` + `pinyin` filter
- 定时全量同步（10 分钟）+ 手动 `POST /search/sync`
- `GoodsDoc.createTime` 用 Long（epoch millis）——Spring Data ES 对 LocalDateTime 存 Long 后无法读回

### 5. Bus 动态刷新

- 所有服务接入 `spring-cloud-starter-bus-amqp` + actuator
- 改配置 → push gitee → `POST /actuator/busrefresh` → 全服务热刷新（零重启，已验证）

## 四、测试工作流

### 环境准备

| 中间件 | 地址 | 凭据 |
|---|---|---|
| MySQL | 127.0.0.1:3306 | root / SQL123456 |
| Nacos | 127.0.0.1:8848 | - |
| Redis | 192.168.89.101:6379 | Redis123456 |
| ES | 192.168.89.101:9200 | -（IK+拼音插件） |
| RabbitMQ | 192.168.89.101:5672 | user / 123456 |

### 启动顺序

```
config(10010) → 10 个业务服务 → gateway(10011)
```

### 验证链路（curl 流程）

```
注册 → 登录 → 鉴权(info) → 商品(增/上架/查) → 加购 → 购物车 → 下单 → 支付/取消(库存回补)
秒杀：建活动 → 抢购 → 结果查询 → 并发防超卖验证
评论/上传/短信/站内信/搜索(中文+拼音)/后台管理(admin token)
Bus：改路由 → push → busrefresh → 不重启生效
滑动续期：请求后 Redis TTL 重置（1735 → 1799）
登出：登出后同 token 401
```

### 并发验证方法

```
秒杀防超卖：库存 N，同一 token 并发 2N 次（limitPerUser 放开），成功数应 = N
```

## 五、测试期修复的问题（16 个坑）

| # | 问题 | 修复 |
|---|---|---|
| 1 | config 只返回 jwt（业务配置缺失） | gitee 仓库文件在 `{name}-dev/` 子目录 → `search-paths: '*-dev'` |
| 2 | user 启动失败 `JwtUtil bean not found` | 补 `config/JwtConfig.java` |
| 3 | admin 启动循环依赖 | JwtUtil Bean 移到独立 JwtConfig |
| 4 | 鉴权 401 | token 复制折行（curl 换行破坏）→ 单行完整 token |
| 5 | 加购 3001 | 新增商品默认下架 → 先上架 |
| 6 | 单独编译找不到 common 类 | 本地仓库 common jar 旧 → `-pl unimall-common install` 或 `-am` |
| 7 | HTTP 方法用错 | 注意 POST/GET 区分 |
| 8 | 评论 404 | 网关路由复数/单数不一致 → 改 `/api/comment/**` |
| 9 | 权限漏洞 | 管理/内部接口暴露给普通用户 → `AdminProtectFilter` 403 |
| 10 | 短信 404 | sendmsg 无网关路由 → 补 `/api/sms/**`、`/api/message/**` |
| 11 | 搜索报错（Long→LocalDateTime） | GoodsDoc.createTime 改 Long + 手动转换 + mapping epoch_millis |
| 12 | 用户管理 total=0 | user 缺分页插件（MybatisPlusConfig） |
| 13 | RabbitMQ ACCESS_REFUSED | guest 禁远程 → 建用户 `user/123456` |
| 14 | config busrefresh 405/未暴露 | config 是 Server 不拉共享配置 → management/rabbitmq 写本地 |
| 15 | 未知路径返回 5000 | NoResourceFoundException 被兜底 → 各服务单独处理 404 |
| 16 | 购物车唯一索引冲突 | 逻辑删除占索引 → 改物理删除（deletePhysical） |

## 六、文档体系

| 文档 | 位置 | 内容 |
|---|---|---|
| AGENTS.md | 根 + 14 模块 | 项目约定/架构/模块要点（精炼，AI 开发参考） |
| TESTING.md | 根 | 测试步骤 + 进度 + 踩坑（本文件第五节的详细版） |
| WORKFLOW.md | 根（本文档） | 开发+测试完整工作流 |
| {模块}-module.md | 各模块 resources/docs/ | 模块详细说明（接口/实现思路/配置） |

## 七、项目状态与后续

**已完成**：14 模块实现 + 全链路测试 + Bus 热刷新 + Token 滑动续期 + 登出 + 权限保护。

**剩余 WIP**：
- 单元测试（项目无测试框架，可引入 JUnit/Mockito）
- 生产化项：Feign 熔断降级、RBAC 权限分级、统计概览、限流（Sentinel）、README
- 部署（Docker 化 / CI）

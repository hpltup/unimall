# unimall-service-user 用户模块说明

> 商城系统用户服务：注册、登录（JWT 签发）、用户信息查询，配合网关完成统一鉴权。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、数据库设计](#三数据库设计)
- [四、接口清单](#四接口清单)
- [五、功能实现思路](#五功能实现思路)
- [六、鉴权链路（与网关协作）](#六鉴权链路与网关协作)
- [七、公共组件](#七公共组件)
- [八、目录结构](#八目录结构)
- [九、配置说明](#九配置说明)
- [十、启动前提](#十启动前提)

---

## 一、模块定位

`unimall-service-user` 是商城微服务集群中的**用户服务**，职责：

- 用户注册（唯一性校验 + 密码加密）
- 用户登录（校验密码、签发 JWT、写入 Redis 白名单）
- 用户信息查询（从网关透传的 `X-User-Id` 获取当前用户）

它向 Nacos 注册，由网关以 `lb://unimall-service-user` 路由转发；配置从 `unimall-config`（Spring Cloud Config Server）拉取。

## 二、技术栈

| 组件 | 说明 |
|---|---|
| Spring Boot 3.3.7 | 基础框架 |
| Spring Cloud 2023.0.1 | 微服务体系 |
| Nacos (spring-cloud-starter-alibaba-nacos-discovery) | 服务注册与发现 |
| Spring Cloud Config (spring-cloud-starter-config) | 配置中心客户端 |
| MyBatis-Plus 3.5.5 | ORM（`IService`/`ServiceImpl`、`BaseMapper`、`@TableLogic` 逻辑删除） |
| MySQL 8 | 数据存储 |
| Redis (spring-boot-starter-data-redis) | 登录 token 白名单 |
| JWT (jjwt 0.12.6) | 无状态令牌 |
| BCrypt (spring-security-crypto) | 密码加密 |
| Lombok | 样板代码 |
| Validation | `@Valid` 参数校验 |

## 三、数据库设计

建表脚本：`src/main/resources/sql/user.sql`（建库 `unimall` + 建表 `user`）

### user 表字段

| 分组 | 字段 | 类型 | 说明 |
|---|---|---|---|
| 账号 | `username` | VARCHAR(32) 唯一 | 登录凭据 |
| | `password` | VARCHAR(100) | BCrypt 加密存储 |
| 登录凭据扩展 | `phone` / `email` | VARCHAR(20)/VARCHAR(64) 唯一 | 可作登录凭据（本版登录仅用 username） |
| 资料 | `nickname` / `avatar` / `gender` / `birthday` | - | 基础资料 |
| 实名 | `real_name` / `id_card` | VARCHAR(32) | 实名认证，`id_card` 脱敏存储 |
| 资产 | `balance` DECIMAL(10,2) / `points` INT / `level` TINYINT | - | 余额 / 积分 / 会员等级 |
| 状态 | `status` TINYINT | 0 禁用 / 1 正常 | 登录时校验 |
| 登录审计 | `last_login_time` / `last_login_ip` | - | 登录成功后更新 |
| 通用 | `create_time` / `update_time` / `deleted` | - | 自动填充 + 逻辑删除（`@TableLogic`） |

索引：主键 + `uk_username` / `uk_phone` / `uk_email` 三个唯一索引。

## 四、接口清单

网关统一前缀 `/api`，`StripPrefix=1` 后落到服务端 `/user/**`。

### 1. 注册 — `POST /user/register`

**入参** `RegisterDTO`：

| 字段 | 校验 | 说明 |
|---|---|---|
| username | `@NotBlank` + 正则 `^[a-zA-Z]\w{3,19}$` | 字母开头，4~20 位 |
| password | `@NotBlank` + 6~32 位 | BCrypt 加密存储 |
| nickname | 可选 | 缺省时取 username |

**出参**：`Result<Long>`（新用户 id）

**异常**：`1001 用户名已存在`

### 2. 登录 — `POST /user/login`

**入参** `LoginDTO`：`username` / `password`（均 `@NotBlank`）

**出参** `Result<LoginVO>`：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOi...",
    "userId": 1,
    "username": "tom123",
    "nickname": "汤姆",
    "expiresIn": 1800
  }
}
```

**异常**：`1002 用户不存在` / `1003 密码错误` / `1004 账号已被禁用`

### 3. 用户信息 — `GET /user/info`

**入参**：请求头 `X-User-Id`（网关校验通过后附加，服务端不暴露用户自传）

**出参** `Result<UserVO>`：`id / username / nickname / phone / email / avatar / gender / birthday / level / createTime`（**不含 password 等敏感字段**）

**异常**：`1002 用户不存在`

### 全局异常码

| code | 含义 |
|---|---|
| 0 | 成功 |
| 1001 | 用户名已存在 |
| 1002 | 用户不存在 |
| 1003 | 密码错误 |
| 1004 | 账号已被禁用 |
| 1005 | 参数校验失败 |
| 5000 | 系统异常 |

## 五、功能实现思路

### 注册

```
Controller(@Valid 校验) → IUserService.register
  1. lambdaQuery 按 username 查重 → 存在抛 1001
  2. BCryptPasswordEncoder.encode() 加密密码
  3. save 入库（create_time/update_time 走数据库默认值）
  4. 返回自增 id
```

> 唯一性校验 + 数据库唯一索引双保险；密码任何时刻不落明文。

### 登录

```
Controller → IUserService.login
  1. 按 username 查用户 → 空抛 1002
  2. passwordEncoder.matches(明文, 密文) → 不匹配抛 1003
  3. status == 0 → 抛 1004
  4. JwtUtil.generateToken(userId) 签发 JWT（payload: sub=userId, jti=UUID, iat, exp）
  5. 解析 token 取 jti，写 Redis：SET login:token:{jti} = userId，TTL = expire-seconds(1800s)
  6. 返回 token / userId / expiresIn
```

> Redis 白名单模式：token 的有效性由"JWT 验签 + Redis 存在性"双重保证，登出/拉黑删 key 即可主动失效。

### 用户信息

```
Controller 从请求头 X-User-Id 取 userId → IUserService.info
  getById 查询 → 空抛 1002 → 转 UserVO（字段白名单，不含密码）
```

> `X-User-Id` 由网关鉴权过滤器在 JWT + Redis 校验通过后附加，服务端信任内网转发，不再自行验签。

## 六、鉴权链路（与网关协作）

```
登录成功(user) → JWT(jti) → Redis: login:token:{jti}=userId (TTL 1800s)
                ↓
用户请求 → 网关 AuthGlobalFilter(-100)
          ├─ 白名单(/api/user/login|register) → 放行
          └─ Authorization: Bearer <token>
             → 验签 → 查 Redis key 存在?
             → 放行(附加 X-User-Id) / 401
                ↓
         user 服务 /info 读 X-User-Id
```

- 网关 `AuthGlobalFilter`（`unimall-gateway` 模块）负责统一鉴权
- 服务端通过 `X-User-Id` 头识别用户，避免重复验签
- 登出：删除 `login:token:{jti}`（后续在退出登录接口实现）

## 七、公共组件

来自 `unimall-common` 模块（纯 Java，无 Spring 依赖）：

| 组件 | 说明 |
|---|---|
| `com.unimall.common.utils.JwtUtil` | JWT 签发/解析（HS256，`jti`/`sub`），`getExpireSeconds()` 提供 TTL |
| `com.unimall.common.result.Result` | 统一返回体 `{code, message, data}` |
| `com.unimall.common.exception.BusinessException` | 业务异常（code + message） |

`GlobalExceptionHandler`（`@RestControllerAdvice`，user 模块内）：`BusinessException` / 参数校验异常 / 兜底异常 三类统一转 `Result`。

## 八、目录结构

```
unimall-service-user/src/main/java/com/unimall/user/
├── UserApplication.java              # 启动类（@SpringBootApplication + @MapperScan）
├── controller/
│   └── UserController.java           # /user/register|login|info
├── service/
│   ├── IUserService.java             # 服务接口
│   └── impl/
│       └── UserServiceImpl.java      # 实现（继承 ServiceImpl）
├── mapper/
│   └── IUserMapper.java              # 继承 BaseMapper<User>
├── pojo/
│   ├── entity/User.java              # 实体（@TableName("`user`")）
│   ├── dto/RegisterDTO, LoginDTO     # 入参
│   └── vo/LoginVO, UserVO            # 出参
└── config/
    └── GlobalExceptionHandler.java   # 全局异常
```

资源文件：

```
unimall-service-user/src/main/resources/
├── application.yml                   # 引导配置（端口/nacos/config 客户端）
├── sql/user.sql                      # 建库建表脚本
└── docs/user-module.md               # 本文档
```

## 九、配置说明

### 本地 `application.yml`（引导配置）

```yaml
server:
  port: 10012
spring:
  application:
    name: unimall-service-user
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    config:
      name: user
      profile: dev
  config:
    import: configserver:http://127.0.0.1:10010
```

### 配置中心 `config-repo/user-dev.yml`（业务配置）

- `spring.datasource`：MySQL `127.0.0.1:3306/unimall`（root / SQL123456）
- `spring.data.redis`：`192.168.89.101:6379`（Redis123456）
- `mybatis-plus`：SQL 日志、逻辑删除字段 `deleted`

### 配置中心 `config-repo/application.yml`（共享配置）

- `unimall.jwt.secret` / `expire-seconds`（1800s）：**所有服务共用同一密钥**（user 签发、网关校验），生产用环境变量 `JWT_SECRET` 注入

## 十、启动前提

1. MySQL 启动，执行 `sql/user.sql` 建库建表
2. Nacos 启动（127.0.0.1:8848）
3. Redis 启动（192.168.89.101:6379）
4. `unimall-config` 启动（native 模式提供 `user-dev.yml` / `application.yml`）
5. 按需启动 `unimall-gateway`（路由 `/api/user/**` → `lb://unimall-service-user`）

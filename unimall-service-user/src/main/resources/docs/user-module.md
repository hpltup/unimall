# unimall-service-user 用户模块说明

> 商城系统用户服务：注册、登录（JWT 签发）、用户信息查询，配合网关完成统一鉴权；并向外提供后台管理内部接口。

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
- [十、启动前提与已知问题](#十启动前提与已知问题)

---

## 一、模块定位

用户服务（端口 **10012**），职责：

- 用户注册（唯一性校验 + 密码加密）
- 用户登录（校验密码、签发 JWT、写入 Redis 白名单）
- 用户信息查询（从网关透传的 `X-User-Id` 获取当前用户）
- 后台管理内部接口（用户分页、禁用/启用，供 admin 服务调用）

## 二、技术栈

| 组件 | 说明 |
|---|---|
| Spring Boot 3.3.7 + Spring Cloud 2023.0.1 | 基础框架 |
| Nacos (nacos-discovery) | 服务注册与发现 |
| Spring Cloud Config | 配置中心客户端 |
| MyBatis-Plus 3.5.5 | ORM（`IService`/`ServiceImpl`、`@TableLogic`） |
| MySQL 8 / Redis | 数据存储 / token 白名单 |
| jjwt 0.12.6 + BCrypt | JWT 签发 / 密码加密 |

## 三、数据库设计

建表脚本：`src/main/resources/sql/user.sql`（建库 `unimall` + 建表 `user`）

| 分组 | 字段 | 说明 |
|---|---|---|
| 账号 | `username`(唯一) / `password` | BCrypt 加密存储 |
| 登录凭据扩展 | `phone` / `email`（唯一） | 可作登录凭据（本版登录仅用 username） |
| 资料 | `nickname` / `avatar` / `gender` / `birthday` | 基础资料 |
| 实名 | `real_name` / `id_card` | 实名认证，`id_card` 脱敏 |
| 资产 | `balance` / `points` / `level` | 余额 / 积分 / 会员等级 |
| 状态 | `status` | 0 禁用 / 1 正常（登录时校验） |
| 审计 | `last_login_time` / `last_login_ip` | 登录成功后更新 |
| 通用 | `create_time` / `update_time` / `deleted` | 自动填充 + 逻辑删除 |

索引：主键 + `uk_username` / `uk_phone` / `uk_email`。

## 四、接口清单

网关统一前缀 `/api`，`StripPrefix=1` 后落到服务端 `/user/**`。

### C 端接口

**1. 注册 — `POST /user/register`**（白名单放行）

入参 `RegisterDTO`：`username`（`@Pattern ^[a-zA-Z]\w{3,19}$`）/ `password`（6~32 位）/ `nickname`（可选）
出参：`Result<Long>`（新用户 id）；异常：`1001 用户名已存在`

**2. 登录 — `POST /user/login`**（白名单放行）

入参 `LoginDTO`：`username` / `password`
出参 `Result<LoginVO>`：`{ token, userId, username, nickname, expiresIn }`
异常：`1002 用户不存在` / `1003 密码错误` / `1004 账号已被禁用`

**3. 用户信息 — `GET /user/info`**（需登录）

入参：请求头 `X-User-Id`（网关附加）
出参 `Result<UserVO>`（不含 password）；异常：`1002 用户不存在`

### 内部管理接口（admin 服务经 Feign 调用，不走网关）

**4. 用户分页 — `GET /user/internal/admin-list?pageNum&pageSize&keyword`**
出参 `Result<Page<UserVO>>`（keyword 模糊匹配 username/nickname）

**5. 禁用/启用 — `PUT /user/internal/admin-status`**
入参 `UserStatusDTO`：`{id, status}`（0禁用 1正常）

## 五、功能实现思路

### 注册

```
Controller(@Valid) → IUserService.register
  → lambdaQuery 查重（重则 1001）→ BCrypt 加密 → save → 返回 id
```

### 登录

```
Controller → IUserService.login
  → 查用户（空 1002）→ matches 校验（错 1003）→ status 校验（禁用 1004）
  → JwtUtil.generateToken(userId)（payload: sub=userId, jti=UUID, iat, exp）
  → Redis SET login:token:{jti}=userId（TTL = expire-seconds 1800s）
  → 返回 token / userId / expiresIn
```

### 用户信息 / 内部管理

- `info`：从 `X-User-Id` 头取 userId → getById → 转 UserVO（字段白名单）
- `adminPage`：`like` username/nickname 分页
- `adminUpdateStatus`：按 id 改 status

## 六、鉴权链路（与网关协作）

```
登录成功(user) → JWT(jti) → Redis: login:token:{jti}=userId (TTL 1800s)
用户请求 → 网关 AuthGlobalFilter(-100)
  ├─ 白名单(/api/user/login|register) → 放行
  └─ 校验 Bearer → 验签 → 查 Redis → 放行(附加 X-User-Id) / 401
user 服务 /info 读 X-User-Id
```

## 七、公共组件

来自 `unimall-common`（纯 Java）：`JwtUtil`（签发/解析）、`Result`（统一返回体）、`BusinessException`（业务异常）、`UserVO`（**跨服务共享出参**，在 `common.vo`）。

`LoginVO` 留在本模块（仅 user 服务使用）。

## 八、目录结构

```
com.unimall.user/
├── UserApplication.java          # @SpringBootApplication + @MapperScan
├── controller/UserController.java
├── service/IUserService.java + impl/UserServiceImpl.java
├── mapper/IUserMapper.java
├── pojo/entity/User.java、dto/RegisterDTO, LoginDTO, UserStatusDTO、vo/LoginVO
└── config/GlobalExceptionHandler.java
```

## 九、配置说明

- 本地 `application.yml`：端口 10012、Nacos、`config.import: configserver://10010`
- 配置中心 `config-repo/user-dev.yml`：datasource + Redis + MyBatis-Plus
- 共享 `config-repo/application.yml`：`unimall.jwt.secret` / `expire-seconds`

## 十、启动前提与已知问题

- 前提：MySQL 建表、Nacos、Redis、config native 激活
- 已知问题：**登出接口未实现**（机制已预留：登出 = 删除 Redis `login:token:{jti}`）

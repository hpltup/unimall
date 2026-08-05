# unimall-service-user — 用户服务

用户服务（端口 **10012**）：注册、登录（签发 JWT + 写 Redis 白名单）、用户信息查询。是其他业务模块（cart/order/comments...）的参照模板。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /user/register` | 注册（username 唯一 + BCrypt + 返回 id） | 白名单放行 |
| `POST /user/login` | 登录（校验密码 → 签发 JWT → Redis `login:token:{jti}` TTL=expire-seconds → 返回 token） | 白名单放行 |
| `GET /user/info` | 用户信息（**从请求头 `X-User-Id` 取 userId**，网关附加，服务端信任） | 需登录 |
| `GET /user/internal/admin-list` | **内部接口**（admin 调）：用户分页（用户名/昵称模糊） |
| `PUT /user/internal/admin-status` | **内部接口**（admin 调）：禁用/启用 |

## 分层与命名（新模块照此模式）

```
com.unimall.user/
├── UserApplication.java      # @SpringBootApplication + @MapperScan("com.unimall.user.mapper")
├── controller/               # 只做参数接收 + Result 包装
├── service/IUserService.java # 接口（I 开头）
├── service/impl/UserServiceImpl.java  # 继承 ServiceImpl<IUserMapper, User>
├── mapper/IUserMapper.java   # 继承 BaseMapper<User>（I 开头）
├── pojo/entity|dto|vo/       # 实体 / 入参(@Valid) / 出参(白名单，不含 password)
└── config/GlobalExceptionHandler.java # @RestControllerAdvice
```

## 要点

- 错误码：`1001 用户名已存在` / `1002 用户不存在` / `1003 密码错误` / `1004 账号已被禁用` / `1005 参数校验失败` / `5000 系统异常`
- 密码 BCrypt（`spring-security-crypto`，不引完整 security）；`id_card` 脱敏
- 实体：`@TableName("`user`")`（user 是保留字）、`@TableId(AUTO)`、`@TableLogic`（deleted）、时间字段走数据库默认值
- Redis：MVC 服务用 **`StringRedisTemplate`**；key 前缀 `login:token:` 与网关一致
- 建表脚本 `src/main/resources/sql/user.sql`（建库 `unimall` + 建表）
- 业务配置在配置中心 `user-dev.yml`（datasource/redis/mp），本地 `application.yml` 只留引导
- MyBatis-Plus 用 **3.5.5**（3.5.17 模块重构，`ServiceImpl` 不在 extension，勿升回）

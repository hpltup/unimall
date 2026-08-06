# unimall-service-admin — 后台管理服务

后台管理服务（端口 **10021**）：**聚合管理面**，通过 Feign 调各业务服务的管理接口。功能：商品管理、订单管理、用户管理、秒杀活动管理（单管理员，无统计）。

## 接口

### 认证（管理面鉴权在 admin 内部）

| 接口 | 说明 |
|---|---|
| `POST /admin/login` | 登录：BCrypt 校验 → 签发 JWT + 存 Redis `admin:token:{jti}`（TTL 1800s） |
| `POST /admin/logout` | 登出：删 Redis 白名单 key |

> 网关白名单 `/api/admin` **整个管理面放行**，admin 服务内 `AdminAuthInterceptor`（HandlerInterceptor）校验 Bearer token + Redis 白名单——**admin 服务必须保证不经网关不可达**。

### 管理功能（薄控制器，转发 Feign）

| 域 | 接口 | 目标服务 |
|---|---|---|
| 商品 | `GET /admin/goods/list`（pageNum/pageSize/keyword/status）、`PUT /admin/goods/status` | goods |
| 订单 | `GET /admin/order/list`（可按 status）、`PUT /admin/order/ship/{id}`、`POST /admin/order/cancel/{id}` | order |
| 用户 | `GET /admin/user/list`、`PUT /admin/user/status` | user |
| 秒杀 | `POST /admin/seckill/activity`、`GET /admin/seckill/list` | seckill |

## 分层

```
com.unimall.admin/
├── AdminApplication.java       # @SpringBootApplication + @MapperScan + @EnableFeignClients
├── client/IGoodsClient, IOrderClient, IUserClient, ISeckillClient   # Feign 管理客户端
├── controller/AdminAuthController, AdminGoodsController, AdminOrderController, AdminUserController, AdminSeckillController
├── service/IAdminAuthService.java + impl/AdminAuthServiceImpl.java  # 登录/登出（JWT + Redis）
├── mapper/IAdminUserMapper.java
├── pojo/entity/AdminUser.java
├── pojo/dto/AdminLoginDTO.java, pojo/vo/AdminLoginVO.java
└── config/
    ├── AdminAuthInterceptor.java    # 管理面鉴权拦截器
    ├── AdminInitializer.java        # 启动创建初始管理员（admin/123456）
    ├── JwtConfig.java               # JwtUtil Bean（独立配置类，避免与拦截器循环依赖）
    ├── WebConfig.java               # 拦截器注册（放行 /admin/login）
    ├── GlobalExceptionHandler.java + MybatisPlusConfig.java
```

## 要点

- 错误码：`9001 管理员不存在` / `9002 密码错误` / `9003 管理员已被禁用` / `401` / `1005` / `5000`
- **初始管理员**：`AdminInitializer`（CommandLineRunner）在 admin_user 表为空时自动创建 `admin / 123456`（BCrypt），SQL 不硬编码密码
- 管理员 token 与 C 端隔离：Redis key 前缀 `admin:token:`（C 端是 `login:token:`），共用 `unimall.jwt` 密钥
- **跨服务契约**：所有 Feign 消费的 DTO/VO（OrderVO/UserVO/SeckillActivityVO/GoodsStatusDTO/UserStatusDTO/SeckillActivityCreateDTO）都在 `unimall-common`，不在业务模块
- `OrderVO`/`UserVO` 等经 Feign 反序列化依赖 MyBatis-Plus 的 `Page`（admin 已引 MP）
- 业务配置在配置中心 `admin-dev.yml`（datasource + redis + mp）

## 已知问题/后续

- 单管理员无 RBAC（表结构可扩展角色字段）
- 统计概览未做（后续按需）
- Feign 无超时/熔断：下游服务不可用时管理接口 500

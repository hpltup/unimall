# unimall-service-admin 后台管理模块说明

> 后台管理服务：**聚合管理面**，通过 Feign 调各业务服务的管理接口。功能：商品/订单/用户/秒杀活动管理（单管理员）。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、数据库设计](#三数据库设计)
- [四、鉴权方案](#四鉴权方案)
- [五、接口清单](#五接口清单)
- [六、目录结构](#六目录结构)
- [七、配置说明](#七配置说明)
- [八、启动前提与已知问题](#八启动前提与已知问题)

---

## 一、模块定位

后台管理服务（端口 **10021**）：管理员登录 + 聚合各业务服务的**内部管理接口**：

```
admin（聚合层）
 ├─ 商品管理 → goods（list/status）
 ├─ 订单管理 → order（internal/admin-*）
 ├─ 用户管理 → user（internal/admin-*）
 └─ 秒杀活动管理 → seckill（activity/list）
```

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / MyBatis-Plus 3.5.5 / MySQL 8 / Redis（管理员 token 白名单）/ OpenFeign + LoadBalancer / BCrypt。

## 三、数据库设计

建表脚本：`src/main/resources/sql/admin.sql`

**admin_user**：`id / username(唯一) / password(BCrypt) / status(0禁用 1正常) / 通用字段`

> **初始账号 `admin/123456` 由 `AdminInitializer`（CommandLineRunner）在表为空时自动创建**（BCrypt 加密），SQL 不硬编码密码。

## 四、鉴权方案

- 网关白名单加 `/api/admin`（**整个管理面放行**）
- admin 服务内 `AdminAuthInterceptor`（HandlerInterceptor）自管鉴权：
  - `POST /admin/login` 放行 → 校验 BCrypt → 签发 JWT + 存 Redis `admin:token:{jti}`（TTL 1800s）
  - 其余请求：解析 Bearer → 验签 → 查 Redis `admin:token:{jti}` 存在才放行，否则 401 JSON
- 管理员 token 与 C 端**隔离**（key 前缀 `admin:token:`，共用 `unimall.jwt` 密钥）
- ⚠️ 安全依赖 admin 内部拦截器：**admin 服务必须保证不经网关不可达**

## 五、接口清单

### 认证

| 接口 | 说明 |
|---|---|
| `POST /admin/login` | 登录 → `{token, adminId, username, expiresIn}` |
| `POST /admin/logout` | 登出（删 Redis 白名单 key） |

### 管理功能（薄控制器，转发 Feign）

| 域 | 接口 | 目标服务 |
|---|---|---|
| 商品 | `GET /admin/goods/list`、`PUT /admin/goods/status` | goods |
| 订单 | `GET /admin/order/list`（可按 status）、`PUT /admin/order/ship/{id}`、`POST /admin/order/cancel/{id}` | order |
| 用户 | `GET /admin/user/list`、`PUT /admin/user/status` | user |
| 秒杀 | `POST /admin/seckill/activity`、`GET /admin/seckill/list` | seckill |

错误码：`9001 管理员不存在` / `9002 密码错误` / `9003 管理员已被禁用` / `401` / `1005` / `5000`。

## 六、目录结构

```
com.unimall.admin/
├── AdminApplication.java          # @SpringBootApplication + @MapperScan + @EnableFeignClients
├── client/IGoodsClient, IOrderClient, IUserClient, ISeckillClient
├── controller/AdminAuthController, AdminGoodsController, AdminOrderController, AdminUserController, AdminSeckillController
├── service/IAdminAuthService.java + impl/AdminAuthServiceImpl.java
├── mapper/IAdminUserMapper.java
├── pojo/entity/AdminUser.java、dto/AdminLoginDTO、vo/AdminLoginVO
└── config/
    ├── AdminAuthInterceptor.java  # 管理面鉴权拦截器
    ├── AdminInitializer.java      # 初始管理员自动创建
    ├── JwtConfig.java             # JwtUtil Bean（独立配置类，避免循环依赖）
    ├── WebConfig.java             # 拦截器注册（放行 /admin/login）
    ├── GlobalExceptionHandler.java + MybatisPlusConfig.java
```

## 七、配置说明

- 本地 `application.yml`：端口 10021、Nacos、config 客户端
- 配置中心 `config-repo/admin-dev.yml`：datasource + Redis（虚拟机）+ MyBatis-Plus
- 跨服务契约（common）：`OrderVO`/`UserVO`/`SeckillActivityVO`（vo）、`GoodsStatusDTO`/`UserStatusDTO`/`SeckillActivityCreateDTO`（dto）
- `OrderVO`/`UserVO` 分页反序列化依赖 MyBatis-Plus `Page`（admin 已引 MP）

## 八、启动前提与已知问题

- 前提：MySQL 建表（admin_user 由启动初始化）、Nacos、Redis、config 启动（git 模式）、**下游服务已启动**（Feign 聚合）
- 已知问题：单管理员无 RBAC（表结构可扩展）；统计概览未做；Feign 无超时/熔断（下游不可用时管理接口 500）

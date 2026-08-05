# unimall-common 公共模块说明

> 微服务集群公共层：统一返回体、业务异常、JWT 工具、**跨服务共享 DTO/VO**。**纯 Java，零 Spring 依赖**，网关（WebFlux）与业务服务（MVC）均可依赖。

## 目录

- [一、模块定位](#一模块定位)
- [二、设计原则](#二设计原则)
- [三、组件清单](#三组件清单)
- [四、使用示例](#四使用示例)
- [五、目录结构](#五目录结构)
- [六、依赖说明](#六依赖说明)

---

## 一、模块定位

`unimall-common` 存放所有微服务共用的基础能力：

| 组件 | 被谁使用 |
|---|---|
| `JwtUtil` | user（签发）、gateway（校验）、admin（管理员签发/校验） |
| `Result` | 所有服务接口 + 网关 401 响应 |
| `BusinessException` | 各业务服务的业务异常 |
| 跨服务 DTO/VO | goods/cart/order/seckill 服务返回，cart/order/search/admin 经 Feign 消费 |

## 二、设计原则

1. **纯 Java，无 Spring 依赖**——不引入框架依赖，网关（WebFlux）与业务服务（MVC）共用不污染类路径
2. **无 Lombok**——POJO 手写 getter/setter（业务模块才用 Lombok）
3. **跨服务 Feign 契约一律放这里**：任何服务间传输的 DTO/VO 不放业务模块，避免跨服务依赖业务 jar

## 三、组件清单

### 基础组件

| 组件 | 说明 |
|---|---|
| `com.unimall.common.result.Result<T>` | 统一返回体 `{code, message, data}`，`Result.ok()/ok(data)/fail(code, message)`，`code=0` 成功 |
| `com.unimall.common.exception.BusinessException` | 业务异常（`code` + `message`），由各服务 `GlobalExceptionHandler` 捕获 |
| `com.unimall.common.utils.JwtUtil` | JWT 工具（HS256）：`generateToken(Long userId)` / `parseToken` / `getJti` / `getUserId` / `getExpireSeconds()`；payload `sub`=userId、`jti`=UUID |

### 跨服务共享 VO（`common.vo`，手写 getter/setter）

| 组件 | 生产者 → 消费者 |
|---|---|
| `GoodsVO` | goods → cart/order/search/admin |
| `CartItemVO` | cart → order |
| `OrderVO` / `OrderItemVO` | order → admin |
| `UserVO` | user → admin |
| `SeckillActivityVO` | seckill → admin |

### 跨服务共享 DTO（`common.dto`）

| 组件 | 说明 |
|---|---|
| `GoodsStockDTO` | 库存操作入参：order/seckill → goods（deduct/restore） |
| `GoodsStatusDTO` | 商品上下架：admin → goods |
| `UserStatusDTO` | 用户禁用/启用：admin → user |
| `SeckillActivityCreateDTO` | 秒杀活动创建：admin → seckill |

## 四、使用示例

```java
// 签发（user/admin 服务）
JwtUtil jwtUtil = new JwtUtil(secret, 1800);
String token = jwtUtil.generateToken(userId);

// 校验（网关/拦截器）
Claims claims = jwtUtil.parseToken(token);
Long userId = jwtUtil.getUserId(claims);

// 统一响应与业务异常
return Result.ok(data);
throw new BusinessException(1001, "用户名已存在");
```

## 五、目录结构

```
unimall-common/src/main/java/com/unimall/common/
├── utils/JwtUtil.java
├── result/Result.java
├── exception/BusinessException.java
├── vo/GoodsVO, CartItemVO, OrderVO, OrderItemVO, UserVO, SeckillActivityVO
└── dto/GoodsStockDTO, GoodsStatusDTO, UserStatusDTO, SeckillActivityCreateDTO
```

## 六、依赖说明

`pom.xml` 仅引入 `jjwt 0.12.6`（api + impl + jackson，版本写死）。

> ⚠️ **改动了本模块，需 `mvn clean install` 到本地仓库**，依赖它的服务才拿得到新代码。

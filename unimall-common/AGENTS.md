# unimall-common — 公共模块

微服务集群公共层。**纯 Java，零 Spring 依赖**——网关（WebFlux）与业务服务（MVC）共用而不污染类路径，禁止引入任何 spring-boot-starter-* 框架依赖。

## 组件清单

| 组件 | 说明 |
|---|---|
| `com.unimall.common.result.Result<T>` | 统一返回体 `{code, message, data}`，`Result.ok()/ok(data)/fail(code, message)`，`code=0` 成功 |
| `com.unimall.common.exception.BusinessException` | 业务异常（`code` + `message`），由各服务 `GlobalExceptionHandler` 捕获 |
| `com.unimall.common.utils.JwtUtil` | JWT 工具（HS256）：`generateToken(Long userId)` / `parseToken` / `getJti` / `getUserId` / `getExpireSeconds()`；payload `sub`=userId、`jti`=UUID |
| `com.unimall.common.vo.GoodsVO` | 商品出参（**跨服务共享**）：goods 服务返回，cart/order 等 Feign 客户端消费。新增跨服务 Feign DTO 一律放 `common.vo`，不放业务模块 |
| `com.unimall.common.vo.CartItemVO` | 购物车条目出参（**跨服务共享**）：cart 返回，order 下单/清购物车消费 |
| `com.unimall.common.dto.GoodsStockDTO` | 库存操作入参（**跨服务共享**）：order/seckill 调 goods 扣/回库存 |

## 约定与注意

- 只引入 `jjwt 0.12.6`（api + impl + jackson，版本在 `pom.xml` 写死，根 pom 未管理）
- **改动了本模块，需 `mvn clean install` 到本地仓库**，依赖它的 gateway/user 等模块才拿得到新代码
- 新增公共能力前先判断：是否所有服务都要用？是否无状态？否则应放业务模块而非这里

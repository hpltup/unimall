# unimall-service-cart 购物车模块说明

> 购物车服务：加入购物车、列表、改数量、删除。**本项目首个使用 OpenFeign 的模块**（校验商品/批量查商品经 Feign 调 goods 服务）。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、数据库设计](#三数据库设计)
- [四、接口清单](#四接口清单)
- [五、功能实现思路](#五功能实现思路)
- [六、跨服务协作](#六跨服务协作)
- [七、目录结构](#七目录结构)
- [八、配置说明](#八配置说明)
- [九、启动前提与已知问题](#九启动前提与已知问题)

---

## 一、模块定位

购物车服务（端口 **10014**）：

- C 端：加入购物车（重复添加合并数量）、列表（含商品信息）、改数量、删除
- 内部：选中的购物车条目（下单用）、批量删除（下单成功后清购物车）

**购物车存数据库**（不存 Redis——Redis 留给秒杀/缓存）。

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / MyBatis-Plus 3.5.5 / MySQL 8 / **OpenFeign + LoadBalancer**。

## 三、数据库设计

建表脚本：`src/main/resources/sql/cart.sql`

**cart**：`id / user_id / goods_id / quantity / checked(0否 1是) / 通用字段`

- 唯一索引 `(user_id, goods_id)`：同一商品重复添加合并数量
- `checked`：下单时只处理选中的条目
- **商品信息不入库**（名称/图/价格来自 goods 服务）

## 四、接口清单

### C 端接口（全部需登录，`X-User-Id` 取用户）

| 接口 | 说明 |
|---|---|
| `POST /cart/add` | 加入购物车 `{goodsId, quantity}`（Feign 校验商品存在且上架；已存在则数量累加） |
| `GET /cart/list` | 购物车列表（Feign 批量查商品，组装 `CartItemVO` 含商品名/图/价/小计） |
| `PUT /cart/quantity` | 改数量 `{id, quantity}` |
| `DELETE /cart/{id}` | 删除条目 |

### 内部接口（order 服务经 Feign 调用）

| 接口 | 说明 |
|---|---|
| `GET /cart/internal/checked?userId=` | 用户选中的条目（下单用） |
| `POST /cart/internal/remove` | 批量删除 `{userId, ids}`（下单成功后清购物车） |

错误码：`3001 商品不存在或已下架` / `3002 购物车条目不存在` / `1005` / `5000`。

## 五、功能实现思路

### 加入购物车

```
Controller(X-User-Id) → add
  → goodsClient.detail(goodsId) 校验存在且 status=1（否则 3001）
  → 按 (userId, goodsId) 查：存在 → quantity 累加；不存在 → 新增（checked=1）
```

### 列表

```
list(userId)
  → 查 cart 条目（update_time 倒序）
  → 收集 goodsIds → goodsClient.batch(ids) 批量查商品
  → 组装 CartItemVO（goodsName/mainImage/price/total = price × quantity）
```

### 内部接口

- `listChecked`：`checked=1` 的条目（同样批量查商品组装）
- `removeBatch`：`eq(userId).in(ids).remove()`（逻辑删除）

## 六、跨服务协作

```
cart → goods：/goods/detail（校验）、/goods/batch（批量查）——Feign 直连，不走网关
order → cart：/cart/internal/checked、/cart/internal/remove
```

## 七、目录结构

```
com.unimall.cart/
├── CartApplication.java           # @SpringBootApplication + @MapperScan + @EnableFeignClients
├── client/IGoodsClient.java       # goods 服务 Feign 客户端
├── controller/CartController.java
├── service/ICartService.java + impl/CartServiceImpl.java
├── mapper/ICartMapper.java
├── pojo/entity/Cart.java、dto/CartAddDTO, CartQuantityDTO
└── config/GlobalExceptionHandler.java
```

## 八、配置说明

- 本地 `application.yml`：端口 10014、Nacos、config 客户端
- 配置中心 `config-repo/cart-dev.yml`：datasource + MyBatis-Plus

## 九、启动前提与已知问题

- 前提：MySQL 建表、Nacos、config native 激活、**goods 服务已启动**（Feign 依赖）
- 已知问题：`IGoodsClient` 无超时/熔断配置——goods 不可用时购物车接口 500，后续可接 `feign.circuitbreaker`
- 购物车全接口需登录：不进网关白名单

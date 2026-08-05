# unimall-service-order 订单模块说明

> 订单服务：核心下单链路，OpenFeign 三服务协作（cart + goods + 本地），订单状态机 + 库存回补，并提供后台管理内部接口。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、数据库设计](#三数据库设计)
- [四、接口清单](#四接口清单)
- [五、功能实现思路](#五功能实现思路)
- [六、下单链路](#六下单链路)
- [七、目录结构](#七目录结构)
- [八、配置说明](#八配置说明)
- [九、启动前提与已知问题](#九启动前提与已知问题)

---

## 一、模块定位

订单服务（端口 **10015**）：

- C 端：从购物车下单、我的订单分页、详情、模拟支付、取消（回库存）
- 内部：全部订单分页、发货、取消任意订单（供 admin 服务调用）

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / MyBatis-Plus 3.5.5 / MySQL 8 / OpenFeign + LoadBalancer。

## 三、数据库设计

建表脚本：`src/main/resources/sql/order.sql`

**orders**（`order` 是 MySQL 保留字，故用复数）：`id / order_no(唯一) / user_id / total_amount / status / pay_time / 通用字段`
**order_item**（商品快照）：`id / order_id / goods_id / goods_name / goods_image / price / quantity / total / 通用字段`

**状态机**：`0待付款 → 1已付款 → 2已完成`；`0待付款 → 3已取消`（取消恢复库存）

> `order_item` 存商品**快照**，不依赖 goods 实时数据。

## 四、接口清单

### C 端接口（需登录，`X-User-Id` 取用户）

| 接口 | 说明 |
|---|---|
| `POST /order/create` | 下单（从购物车选中条目）→ 返回订单 id |
| `GET /order/list?pageNum&pageSize` | 我的订单分页 |
| `GET /order/detail/{id}` | 订单详情（含明细） |
| `POST /order/pay/{id}` | 模拟支付：0→1（记录 pay_time） |
| `POST /order/cancel/{id}` | 取消：0→3 + 恢复库存 |

### 内部接口（admin 服务经 Feign 调用）

| 接口 | 说明 |
|---|---|
| `GET /order/internal/admin-list?pageNum&pageSize&status` | 全部订单分页（可按状态过滤） |
| `POST /order/internal/admin-ship/{id}` | 发货：1→2 |
| `POST /order/internal/admin-cancel/{id}` | 取消任意订单：0→3 + 恢复库存 |

错误码：`4001 订单不存在` / `4002 订单状态不允许` / `4003 购物车没有选中的商品` / `4004 商品库存不足` / `1005` / `5000`。

## 五、功能实现思路

### 下单（`@Transactional` 本地事务）

```
create(userId)
  1. cartClient.listChecked(userId) → 空抛 4003
  2. 逐条 goodsClient.deductStock()（失败 → 回补已扣部分 + 抛 4004）
  3. 本地事务：建 orders + order_item（快照，总价 = Σ price×quantity）
     （异常 → 回补已扣库存）
  4. cartClient.removeBatch() 清购物车（失败不影响订单，仅记录）
  5. 返回订单 id
```

订单号：`yyyyMMddHHmmss` + 4 位随机。

### 支付 / 取消 / 内部管理

- `pay`：校验归属（userId）+ 状态 0 → 1 + payTime
- `cancel` / `adminCancel`：状态 0 → 3 + `restoreOrderStock`（按明细逐个调 goods `/restore`）
- `adminShip`：状态 1 → 2

## 六、下单链路（三服务协作）

```
POST /order/create
  → cart /internal/checked 取选中条目
  → goods /deduct 逐条原子扣库存（防超卖；失败回补）
  → 本地事务建 orders + order_item（快照）
  → cart /internal/remove 清购物车
```

**无分布式事务**（学习项目简化）：扣库存记录已扣、建单失败回补库存兜底；后续可升级 Seata / 本地消息表。

## 七、目录结构

```
com.unimall.order/
├── OrderApplication.java          # @SpringBootApplication + @MapperScan + @EnableFeignClients
├── client/IGoodsClient, ICartClient
├── controller/OrderController.java
├── service/IOrderService.java + impl/OrderServiceImpl.java
├── mapper/IOrderMapper, IOrderItemMapper
├── pojo/entity/Order, OrderItem、pojo/vo/OrderItemVO
└── config/GlobalExceptionHandler, MybatisPlusConfig（分页）
```

## 八、配置说明

- 本地 `application.yml`：端口 10015、Nacos、config 客户端
- 配置中心 `config-repo/order-dev.yml`：datasource + MyBatis-Plus
- 跨服务出参 `OrderVO` / `OrderItemVO` 在 `unimall-common.vo`

## 九、启动前提与已知问题

- 前提：MySQL 建表、Nacos、config native 激活、**goods/cart 已启动**（Feign 依赖）
- 已知问题：Feign 无超时/熔断；支付为模拟（未接真实支付/回调）；无分布式事务（简化方案）

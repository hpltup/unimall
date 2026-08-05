# unimall-service-order — 订单服务

订单服务（端口 **10015**）：核心下单链路，OpenFeign 三服务协作（cart + goods + 本地）。

## 接口（全部需登录，从请求头 `X-User-Id` 取用户）

| 接口 | 说明 |
|---|---|
| `POST /order/create` | 下单：cart 选中条目 → goods **原子扣库存** → 本地建订单+明细（快照）→ cart 清购物车 |
| `GET /order/list?pageNum&pageSize` | 我的订单分页 |
| `GET /order/detail/{id}` | 订单详情（含明细） |
| `POST /order/pay/{id}` | 模拟支付：0待付款 → 1已付款 |
| `POST /order/cancel/{id}` | 取消：0待付款 → 3已取消 + goods 回补库存 |

## 订单状态

`0待付款` → `1已付款` → `2已完成`；`0待付款` → `3已取消`（取消恢复库存）

## 分层与命名（与 user/goods/cart 一致）

```
com.unimall.order/
├── OrderApplication.java        # @SpringBootApplication + @MapperScan + @EnableFeignClients
├── client/
│   ├── IGoodsClient.java        # goods：/goods/deduct、/goods/restore
│   └── ICartClient.java         # cart：/cart/internal/checked、/cart/internal/remove
├── controller/                  # /order/*
├── service/IOrderService.java + impl/OrderServiceImpl.java
├── mapper/IOrderMapper.java + IOrderItemMapper.java
├── pojo/entity/Order.java, OrderItem.java
├── pojo/vo/OrderVO.java, OrderItemVO.java
└── config/GlobalExceptionHandler.java + MybatisPlusConfig.java（分页）
```

## 要点

- 错误码：`4001 订单不存在` / `4002 订单状态不允许` / `4003 购物车没有选中的商品` / `4004 商品库存不足` / `1005` / `5000`
- 表名 **`orders`**（`order` 是 MySQL 保留字）；`order_item` 存商品**快照**（名称/图/单价），不依赖 goods 实时数据
- **防超卖**：扣库存用 goods 的原子 `UPDATE ... SET stock=stock-? WHERE stock>=?`（数据库层），非 Redis（Redis 扣减留给 seckill）
- **无分布式事务**（学习项目简化）：先逐条扣库存（记录已扣，中途失败回补）→ 建订单（本地 `@Transactional`，失败回补库存）→ 清购物车（失败不影响订单）。后续可升级 Seata/本地消息表
- 订单号：`yyyyMMddHHmmss` + 4 位随机
- 跨服务 DTO（`GoodsStockDTO`/`CartItemVO`）在 `unimall-common`，不在业务模块
- 业务配置在配置中心 `order-dev.yml`；MyBatis-Plus 3.5.5（勿升回 3.5.17）

## 已知问题

- Feign 无超时/熔断配置：goods/cart 不可用时下单会 500，后续接 `feign.circuitbreaker`
- 支付为模拟（改状态），未接真实支付/回调

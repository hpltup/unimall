# unimall-service-cart — 购物车服务

购物车服务（端口 **10014**）：加入购物车、列表、改数量、删除。**本项目第一个使用 OpenFeign 的模块**——校验商品/批量查商品信息均经 Feign 调用 goods 服务。

## 接口（全部需登录，从请求头 `X-User-Id` 取用户）

| 接口 | 说明 |
|---|---|
| `POST /cart/add` | 加入购物车：Feign 校验商品存在且上架 → 已存在则累加数量，否则新增 |
| `GET /cart/list` | 购物车列表：查条目 → Feign 批量查商品 → 组装 `CartItemVO`（含商品名/图/价格/小计） |
| `PUT /cart/quantity` | 修改数量（`{id, quantity}`） |
| `DELETE /cart/{id}` | 删除条目 |
| `GET /cart/internal/checked?userId=` | **内部接口**（订单服务调）：用户选中的购物车条目 |
| `POST /cart/internal/remove` | **内部接口**（订单服务调）：批量删除条目（下单后清购物车） |

## 分层与命名（与 user/goods 一致）

```
com.unimall.cart/
├── CartApplication.java       # @SpringBootApplication + @MapperScan + @EnableFeignClients
├── client/IGoodsClient.java   # goods 服务 Feign 客户端（服务间直连，不走网关）
├── controller/                # /cart/*
├── service/ICartService.java + impl/CartServiceImpl.java
├── mapper/ICartMapper.java
├── pojo/entity/Cart.java      # 购物车条目
├── pojo/dto|vo/               # CartAddDTO / CartQuantityDTO / CartItemVO
└── config/GlobalExceptionHandler.java
```

## 要点

- 错误码：`3001 商品不存在或已下架` / `3002 购物车条目不存在` / `1005` / `5000`
- 表 `cart`：`(user_id, goods_id)` 唯一索引（重复添加合并数量），`checked` 选中标记（下单时用）
- **删除为物理删除**（`ICartMapper.deletePhysical`）：购物车是临时数据，逻辑删除会占用唯一索引导致"删了再加购"冲突（曾踩坑，已改物理 DELETE）
- **商品信息不入库**：名称/图/价格来自 goods 服务，Feign 批量查（`/goods/batch`）
- **跨服务共享 DTO（`GoodsVO`）在 `unimall-common.vo`**，不在业务模块——新增 Feign DTO 同理
- Feign 依赖：`spring-cloud-starter-openfeign` + `spring-cloud-starter-loadbalancer`（2023 系无 Ribbon，必须显式引）
- 购物车全接口需登录：**不进网关白名单**，靠 `X-User-Id` 识别用户
- 业务配置在配置中心 `cart-dev.yml`，本地 `application.yml` 只留引导
- MyBatis-Plus 版本 3.5.5（勿升回 3.5.17）

## 已知问题

- `IGoodsClient` 未配置 Feign 错误处理/超时（默认连接超时），goods 服务不可用时购物车接口会 500——后续接入 `feign.circuitbreaker` 或统一降级

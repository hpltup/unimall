# unimall-service-goods 商品模块说明

> 商品服务：分类管理、商品查询/新增/上下架、库存扣减（防超卖）、服务间内部接口。商品建模为**简化版**（无 SPU/SKU）。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、数据库设计](#三数据库设计)
- [四、接口清单](#四接口清单)
- [五、功能实现思路](#五功能实现思路)
- [六、公共组件](#六公共组件)
- [七、目录结构](#七目录结构)
- [八、配置说明](#八配置说明)
- [九、启动前提与已知问题](#九启动前提与已知问题)

---

## 一、模块定位

商品服务（端口 **10013**）：

- 分类列表（两级分类，启用中）
- 商品分页查询 / 详情 / 新增（默认下架）/ 上下架
- **库存扣减（数据库原子防超卖）/ 回补**（order/seckill 用）
- 服务间内部接口（批量查询、全量上架商品，供 cart/search/admin 调用）

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / MyBatis-Plus 3.5.5 / MySQL 8。

## 三、数据库设计

建表脚本：`src/main/resources/sql/goods.sql`（`category` + `goods` + 3 条初始分类）

**category**：`id / name / parent_id(0为一级) / sort / status / 通用字段`
**goods**：`id / category_id / name / sub_title / main_image / images(JSON) / detail / price / market_price / stock / sales / status(0下架 1上架) / 通用字段`

索引：goods 建 `idx_category_id`、`idx_status`。

## 四、接口清单

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `GET /category/list` | 启用分类（sort 升序） | 白名单 |
| `GET /goods/list?pageNum&pageSize&categoryId&keyword&status` | 分页查询（keyword 模糊 name） | 白名单 |
| `GET /goods/detail/{id}` | 商品详情 | 白名单 |
| `POST /goods` | 新增（**默认下架 status=0**） | 需登录 |
| `PUT /goods/status` | 上下架 `{id, status}` | 需登录 |
| `GET /goods/batch?ids=` | **内部**：批量查询（cart/order 用） | 服务间 |
| `POST /goods/deduct` | **内部**：扣库存（原子防超卖，不足抛 2002） | 服务间 |
| `POST /goods/restore` | **内部**：回补库存 | 服务间 |
| `GET /goods/internal/for-search` | **内部**：全部上架商品（search 同步 ES 用） | 服务间 |

错误码：`2001 商品不存在` / `2002 商品库存不足` / `1005` / `5000`。

## 五、功能实现思路

### 分页查询

`Page<GoodsVO>` 由 `Page<Goods>` 转换（依赖 `MybatisPlusConfig` 分页插件），条件：categoryId / keyword(name like) / status，`create_time` 倒序。

### 扣库存（防超卖，数据库原子）

```java
lambdaUpdate()
    .setSql("stock = stock - " + quantity)
    .eq(Goods::getId, goodsId)
    .ge(Goods::getStock, quantity)   // 条件保证库存足够
    .update();                       // 影响行数 0 → 库存不足抛 2002
```

单条 SQL 原子执行，并发下不可能超卖（Redis 扣减留给 seckill 阶段）。

### 新增

默认 `status=0`（下架），审核后调 `/goods/status` 上架。

## 六、公共组件

- `GoodsVO` 在 `unimall-common.vo`（**跨服务共享**：cart/order/search/admin 消费）
- `GoodsStockDTO` / `GoodsStatusDTO` 在 `unimall-common.dto`

## 七、目录结构

```
com.unimall.goods/
├── GoodsApplication.java          # @SpringBootApplication + @MapperScan
├── controller/CategoryController, GoodsController
├── service/ICategoryService, IGoodsService + impl/
├── mapper/ICategoryMapper, IGoodsMapper
├── pojo/entity/Category, Goods、dto/GoodsQueryDTO, GoodsCreateDTO
├── pojo/vo/CategoryVO
└── config/GlobalExceptionHandler, MybatisPlusConfig（分页插件）
```

## 八、配置说明

- 本地 `application.yml`：端口 10013、Nacos、config 客户端
- 配置中心 `config-repo/goods-dev.yml`：datasource + MyBatis-Plus（逻辑删除 `deleted`）

## 九、启动前提与已知问题

- 前提：MySQL 建表、Nacos、config 启动（git 模式）
- 已知问题：商品浏览的 Redis 缓存未做（缓存穿透/击穿处理留待后续）；库存扣减在数据库层（order 用），秒杀用 Redis（**秒杀库存已联动扣减 goods 库存**，见 seckill 模块）

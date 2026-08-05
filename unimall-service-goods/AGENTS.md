# unimall-service-goods — 商品服务

商品服务（端口 **10013**）：分类管理、商品查询/新增/上下架。商品建模为**简化版**（category + goods 单表，无 SPU/SKU 拆分，`unimall-item` 已并入本模块）。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `GET /category/list` | 启用中的分类列表（sort 升序） | 白名单放行 |
| `GET /goods/list` | 分页查询（categoryId / keyword 名称模糊 / status 条件，create_time 倒序） | 白名单放行 |
| `GET /goods/detail/{id}` | 商品详情 | 白名单放行 |
| `POST /goods` | 新增商品（**默认下架 status=0**，审核后上架） | 需登录 |
| `PUT /goods/status` | 上下架（`{id, status}`） | 需登录 |
| `GET /goods/batch?ids=1,2,3` | **内部批量查询**（cart/order 经 Feign 直连调用，不走网关） | 服务间 |
| `POST /goods/deduct` | **内部扣减库存**（原子 `UPDATE stock=stock-? WHERE stock>=?` 防超卖，不足抛 2002；order/seckill 用） | 服务间 |
| `POST /goods/restore` | **内部回补库存**（订单取消时） | 服务间 |
| `GET /goods/internal/for-search` | **内部全量上架商品**（search 服务同步到 ES 用） | 服务间 |

## 分层与命名（与 user 模块一致）

```
com.unimall.goods/
├── GoodsApplication.java      # @SpringBootApplication + @MapperScan("com.unimall.goods.mapper")
├── controller/                # 只做参数接收 + Result 包装
├── service/ICategoryService.java, IGoodsService.java  # 接口（I 开头）
├── service/impl/…ServiceImpl.java  # 继承 ServiceImpl<IMapper, Entity>
├── mapper/ICategoryMapper.java, IGoodsMapper.java     # 继承 BaseMapper（I 开头）
├── pojo/entity|dto|vo/        # 实体 / 入参(@Valid) / 出参
└── config/
    ├── GlobalExceptionHandler.java  # @RestControllerAdvice
    └── MybatisPlusConfig.java       # 分页插件（PaginationInnerInterceptor，MYSQL）
```

## 要点

- 错误码：`2001 商品不存在` / `1005 参数校验失败` / `5000 系统异常`（沿用 user 风格）
- **分页必须依赖 `MybatisPlusConfig` 的分页插件**（`Page<GoodsVO>` 由 `Page<Goods>` 转换，VO 层不暴露 entity）
- 实体：`@TableName("category"/"goods")`、`@TableId(AUTO)`、`@TableLogic`（deleted）、时间字段走数据库默认值；`images` 为 JSON 字符串存轮播图
- 管理接口（新增/上下架）目前仅靠网关鉴权（`X-User-Id`），**后台管理员权限校验待 admin 模块接入**
- 建表脚本 `src/main/resources/sql/goods.sql`（建 `category` + `goods` + 3 条初始分类）
- 业务配置在配置中心 `goods-dev.yml`（datasource + MyBatis-Plus），本地 `application.yml` 只留引导
- **商品浏览（list/detail/category）已在网关白名单**（`gateway-dev.yml`），无需 token
- 库存扣减（防超卖）/ 商品 Redis 缓存待 order/seckill 阶段实现，本模块当前不含 Redis 依赖
- MyBatis-Plus 版本 3.5.5（勿升回 3.5.17，`ServiceImpl` 不在 extension）

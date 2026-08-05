# UniMall 微服务商城

基于 **Spring Cloud Alibaba** 的微服务商城系统（毕业设计/实习项目）。**14 个模块已全部实现**（编译通过），当前处于整套环境启动与功能验证阶段。

## 文档导航

- [工程约定（AGENTS.md）](AGENTS.md) — 技术栈、架构链路、代码规范、已知缺口（供 AI 编码代理与开发者）
- [测试步骤（TESTING.md）](TESTING.md) — 整套环境启动与接口功能验证（含 curl 命令）

## 模块导航

> 每个模块目录含两份文档：`AGENTS.md`（AI 工程约定）与 `{模块}-module.md`（模块说明笔记）。

### 基础设施

| 模块 | 端口 | 职责 | 说明文档 |
|---|---|---|---|
| `unimall-common` | -（纯库） | 公共组件：Result / 业务异常 / JWT / 跨服务 VO、DTO，零 Spring 依赖 | [common-module.md](unimall-common/common-module.md) |
| `unimall-config` | 10010 | 配置中心（Config Server，`config-repo/` 本地仓库） | [config-module.md](unimall-config/config-module.md) |
| `unimall-registry` | 8080 | Nacos Server 独立部署（8848）+ 配置客户端壳应用 | [registry-module.md](unimall-registry/registry-module.md) |
| `unimall-gateway` | 10011 | 统一入口：路由（`lb://` + Nacos）、JWT 鉴权、CORS | [gateway-module.md](unimall-gateway/gateway-module.md) |

### 业务服务

| 模块 | 端口 | 职责 | 说明文档 |
|---|---|---|---|
| `unimall-service-user` | 10012 | 用户：注册/登录（JWT + Redis 白名单）/信息查询 | [user-module.md](unimall-service-user/user-module.md) |
| `unimall-service-goods` | 10013 | 商品：分类/商品分页/详情/上下架/原子扣减库存 | [goods-module.md](unimall-service-goods/goods-module.md) |
| `unimall-service-cart` | 10014 | 购物车（Feign 调 goods 校验与批量查询） | [cart-module.md](unimall-service-cart/cart-module.md) |
| `unimall-service-order` | 10015 | 订单：下单/支付/取消（Feign 三服务链路，取消回库存） | [order-module.md](unimall-service-order/order-module.md) |
| `unimall-service-seckill` | 10016 | 秒杀：活动管理/抢购（Redis Lua 防超卖） | [seckill-module.md](unimall-service-seckill/seckill-module.md) |
| `unimall-service-comments` | 10017 | 评论：发表（需登录）/按商品查询/删除 | [comments-module.md](unimall-service-comments/comments-module.md) |
| `unimall-service-upload` | 10018 | 文件上传（本地磁盘存储，UUID 重命名） | [upload-module.md](unimall-service-upload/upload-module.md) |
| `unimall-service-sendmsg` | 10019 | 消息：短信验证码（模拟）/站内信 | [sendmsg-module.md](unimall-service-sendmsg/sendmsg-module.md) |
| `unimall-service-search` | 10020 | 搜索（Elasticsearch + IK/拼音分词，定时全量同步） | [search-module.md](unimall-service-search/search-module.md) |

### 聚合管理

| 模块 | 端口 | 职责 | 说明文档 |
|---|---|---|---|
| `unimall-service-admin` | 10021 | 后台管理（聚合层，Feign 调各服务管理接口） | [admin-module.md](unimall-service-admin/admin-module.md) |

## 当前状态

- **14 个模块全部实现**（编译通过），无空骨架；`unimall-item` 已并入 `unimall-service-goods`
- 整体测试进行中：启动顺序、验证命令与排障见 [TESTING.md](TESTING.md)
- 已知缺口（WIP）：config 未激活 native 模式（测试前需手动配置）等，详见 [AGENTS.md](AGENTS.md)「开发中的已知缺口」

# UniMall 微服务商城

基于 **Spring Cloud Alibaba** 的微服务商城系统。**15 个模块已全部实现**，核心链路（注册→下单→支付/取消→秒杀防超卖→搜索→后台→AI 客服→Bus 热刷新→Token 滑动续期）已完成全链路验证。

## 文档导航

- [架构图（ARCHITECTURE.md）](ARCHITECTURE.md) — 总体架构、Feign 调用关系、鉴权链路、配置刷新
- [工程约定（AGENTS.md）](AGENTS.md) — 技术栈、架构链路、代码规范、已知缺口（供 AI 编码代理与开发者）
- [测试步骤（TESTING.md）](TESTING.md) — 整套环境启动与接口功能验证（含 curl 命令、测试记录与踩坑）
- [Docker 部署（DEPLOY.md）](DEPLOY.md) — 方案 A（仅容器化服务）部署说明
- [开发工作流（WORKFLOW.md）](WORKFLOW.md) — 本次开发流程回顾

## 模块导航

> 每个模块含两份文档：根目录 `AGENTS.md`（AI 工程约定）与 `src/main/resources/docs/{模块}-module.md`（模块说明笔记）。

### 基础设施

| 模块 | 端口 | 职责 | 说明文档 |
|---|---|---|---|
| `unimall-common` | -（纯库） | 公共组件：Result / 业务异常 / JWT / 跨服务 VO、DTO，零 Spring 依赖 | [common-module.md](unimall-common/src/main/resources/docs/common-module.md) |
| `unimall-config` | 10010 | 配置中心（Config Server，**gitee git 模式** + Bus 热刷新） | [config-module.md](unimall-config/src/main/resources/docs/config-module.md) |
| `unimall-registry` | 8080 | Nacos Server 独立部署（8848）+ 配置客户端壳应用 | [registry-module.md](unimall-registry/src/main/resources/docs/registry-module.md) |
| `unimall-gateway` | 10011 | 统一入口：路由（`lb://` + Nacos）、JWT 鉴权（Redis 白名单）、管理接口保护、CORS | [gateway-module.md](unimall-gateway/src/main/resources/docs/gateway-module.md) |

### 业务服务

| 模块 | 端口 | 职责 | 说明文档 |
|---|---|---|---|
| `unimall-service-user` | 10012 | 用户：注册/登录（JWT + Redis 白名单）/信息查询/登出 | [user-module.md](unimall-service-user/src/main/resources/docs/user-module.md) |
| `unimall-service-goods` | 10013 | 商品：分类/分页（名称+副标题模糊搜索）/详情/上下架/原子扣减库存 | [goods-module.md](unimall-service-goods/src/main/resources/docs/goods-module.md) |
| `unimall-service-cart` | 10014 | 购物车（Feign 调 goods 校验与批量查询，下单物理删除） | [cart-module.md](unimall-service-cart/src/main/resources/docs/cart-module.md) |
| `unimall-service-order` | 10015 | 订单：下单/支付/取消（Feign 三服务链路，取消回库存） | [order-module.md](unimall-service-order/src/main/resources/docs/order-module.md) |
| `unimall-service-seckill` | 10016 | 秒杀：活动管理/抢购（Redis Lua 防超卖，联动扣 goods 库存） | [seckill-module.md](unimall-service-seckill/src/main/resources/docs/seckill-module.md) |
| `unimall-service-comments` | 10017 | 评论：发表（需登录）/按商品查询（公开）/删除 | [comments-module.md](unimall-service-comments/src/main/resources/docs/comments-module.md) |
| `unimall-service-upload` | 10018 | 文件上传（本地磁盘存储，UUID 重命名 + 扩展名白名单） | [upload-module.md](unimall-service-upload/src/main/resources/docs/upload-module.md) |
| `unimall-service-sendmsg` | 10019 | 消息：短信验证码（模拟）/站内信 | [sendmsg-module.md](unimall-service-sendmsg/src/main/resources/docs/sendmsg-module.md) |
| `unimall-service-search` | 10020 | 搜索（Elasticsearch + IK/拼音分词，定时全量同步） | [search-module.md](unimall-service-search/src/main/resources/docs/search-module.md) |

### 聚合管理 / 智能化

| 模块 | 端口 | 职责 | 说明文档 |
|---|---|---|---|
| `unimall-service-admin` | 10021 | 后台管理（聚合层，Feign 调各服务管理接口） | [admin-module.md](unimall-service-admin/src/main/resources/docs/admin-module.md) |
| `unimall-service-ai` | 10022 | AI 客服（Spring AI + DeepSeek）：SSE 流式对话 + 工具调用（搜商品/加购/确认后下单/查订单/取消），**支付不代付** | [ai-module.md](unimall-service-ai/src/main/resources/docs/ai-module.md) |

## 当前状态

- **15 个模块全部实现**，无空骨架；`unimall-item` 已并入 `unimall-service-goods`（registry 是 Nacos 部署说明壳、common 是纯库，不部署）
- **全链路测试已完成**：注册→下单→支付/取消→秒杀防超卖（100 并发恰好 100 单）→搜索（IK+拼音）→后台管理→AI 客服（推荐/加购/确认下单/拒代付/取消订单）→Bus 热刷新→Token 滑动续期，验证命令与记录见 [TESTING.md](TESTING.md)
- **Docker 部署（方案 A）已搭建待实测**：13 个服务镜像 + compose 编排，详见 [DEPLOY.md](DEPLOY.md)
- 已知缺口（WIP）：单元测试（项目无测试框架）、前端未开发（CORS 已预留 `localhost:5173`）、生产化项（Feign 熔断 / RBAC / 限流），详见 [AGENTS.md](AGENTS.md)

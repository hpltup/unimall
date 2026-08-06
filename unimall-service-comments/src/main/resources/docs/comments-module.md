# unimall-service-comments 评论模块说明

> 评论服务：商品评论发表/查询/删除，列表公开访问。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、数据库设计](#三数据库设计)
- [四、接口清单](#四接口清单)
- [五、功能实现思路](#五功能实现思路)
- [六、目录结构](#六目录结构)
- [七、配置说明](#七配置说明)
- [八、启动前提与已知问题](#八启动前提与已知问题)

---

## 一、模块定位

评论服务（端口 **10017**）：用户对商品发表评论（含评分 1~5）、按商品浏览评论（公开）、我的评论、删除自己的评论。

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / MyBatis-Plus 3.5.5 / MySQL 8。

## 三、数据库设计

建表脚本：`src/main/resources/sql/comment.sql`

**comment**：`id / goods_id / user_id / content(≤500) / images(JSON) / rating(1~5) / status(0待审核 1显示 2隐藏) / 通用字段`

索引：`idx_goods_id`、`idx_user_id`。

## 四、接口清单

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /comment` | 发表 `{goodsId, content, rating, images?}`（**简化：发表即显示 status=1**） | 需登录 |
| `GET /comment/list?goodsId=&pageNum&pageSize=` | 按商品查**已显示**评论（时间倒序，分页） | **白名单** |
| `GET /comment/list/my` | 我的评论（分页） | 需登录 |
| `DELETE /comment/{id}` | 删除自己的评论（`eq(userId)` 校验） | 需登录 |

错误码：`6001 评论不存在` / `1005` / `5000`。

## 五、功能实现思路

- `create`：组装实体 → save（status=1 直接显示，预留审核状态）
- `pageByGoods`：`eq(goodsId).eq(status,1)` 分页倒序
- `pageMy`：`eq(userId)` 分页
- `remove`：`eq(id).eq(userId)` 查归属 → 删除（只能删自己的）

## 六、目录结构

```
com.unimall.comments/
├── CommentsApplication.java       # @SpringBootApplication + @MapperScan
├── controller/CommentController.java
├── service/ICommentService.java + impl/CommentServiceImpl.java
├── mapper/ICommentMapper.java
├── pojo/entity/Comment.java、dto/CommentCreateDTO、vo/CommentVO
└── config/GlobalExceptionHandler, MybatisPlusConfig（分页）
```

## 七、配置说明

- 本地 `application.yml`：端口 10017、Nacos、config 客户端
- 配置中心 `config-repo/comments-dev.yml`：datasource + MyBatis-Plus
- 网关白名单已含 `/api/comment/list`

## 八、启动前提与已知问题

- 前提：MySQL 建表、Nacos、config 启动（git 模式）
- 已知问题：`CommentVO` 未带用户名/头像（需调 user 服务或评论表冗余快照）；商品平均评分聚合未做；未校验"已购用户才能评论"

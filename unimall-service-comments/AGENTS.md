# unimall-service-comments — 评论服务

评论服务（端口 **10017**）：商品评论发表/查询/删除。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /comment` | 发表评论（`goodsId/content/rating(1~5)/images`） | 需登录 |
| `GET /comment/list?goodsId=&pageNum=&pageSize=` | 按商品查**已显示**评论（时间倒序，分页） | 白名单放行 |
| `GET /comment/list/my` | 我的评论（分页） | 需登录 |
| `DELETE /comment/{id}` | 删除自己的评论 | 需登录 |

## 分层与命名（与 user/goods 一致）

```
com.unimall.comments/
├── CommentsApplication.java   # @SpringBootApplication + @MapperScan
├── controller/                # /comment/*
├── service/ICommentService.java + impl/CommentServiceImpl.java
├── mapper/ICommentMapper.java
├── pojo/entity/Comment.java
├── pojo/dto/CommentCreateDTO.java
├── pojo/vo/CommentVO.java
└── config/GlobalExceptionHandler.java + MybatisPlusConfig.java（分页）
```

## 要点

- 错误码：`6001 评论不存在` / `1005` / `5000`
- 表 `comment`：`rating 1~5`、`status 0待审核 1显示 2隐藏`（**简化：发表默认 1 直接显示**）
- `images` JSON 字符串存图片
- 删除只允许删自己的（`eq(userId)` 校验）
- 评论列表已加入网关白名单（`/api/comment/list`），浏览公开
- 业务配置在配置中心 `comments-dev.yml`；MyBatis-Plus 3.5.5（勿升回 3.5.17）

## 已知问题/后续

- `CommentVO` 未带用户名/头像（需调 user 服务，或评论表冗余用户快照——待前端需要时再加）
- 商品平均评分聚合未做（可后续在 goods 服务统计或展示时前端计算）
- 未校验"已购用户才能评论"（下单校验待接 order 服务后可选）

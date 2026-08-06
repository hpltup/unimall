# UniMall 后期测试步骤（运行验证）

> 适用：全部 14 个模块已实现（仅编译通过），本文件用于**整套环境启动与功能验证**。
> 所有接口统一走网关 `http://localhost:10011`（前缀 `/api`）。

## 一、端口总览

| 服务                       | 端口       | 说明                      |
| ------------------------ | -------- | ----------------------- |
| unimall-config           | 10010    | 配置中心（Config Server）     |
| unimall-gateway          | 10011    | 网关（统一入口）                |
| unimall-service-user     | 10012    | 用户                      |
| unimall-service-goods    | 10013    | 商品                      |
| unimall-service-cart     | 10014    | 购物车                     |
| unimall-service-order    | 10015    | 订单                      |
| unimall-service-seckill  | 10016    | 秒杀                      |
| unimall-service-comments | 10017    | 评论                      |
| unimall-service-upload   | 10018    | 上传                      |
| unimall-service-sendmsg  | 10019    | 短信/站内信                  |
| unimall-service-search   | 10020    | 搜索（ES）                  |
| unimall-service-admin    | 10021    | 后台管理                    |
| unimall-registry         | 8080（默认） | Nacos Server（独立部署，8848） |

## 二、环境准备

| 中间件 | 地址 | 凭据 | 备注 |
|---|---|---|---|
| MySQL | 127.0.0.1:3306 | root / SQL123456 | 需执行建表 SQL |
| Nacos | 127.0.0.1:8848 | - | `startup.cmd -m standalone` |
| Redis | 192.168.89.101:6379 | Redis123456 | 虚拟机 |
| Elasticsearch | 192.168.89.101:9200 | 视安全配置 | 虚拟机，需 IK + 拼音插件 |
| RabbitMQ | 127.0.0.1:5672 | - | 仅 Bus 动态刷新需要（可选） |

### 1. MySQL 建库建表

按顺序执行以下脚本（均在 `unimall` 库）：

```
unimall-service-user/src/main/resources/sql/user.sql
unimall-service-goods/src/main/resources/sql/goods.sql
unimall-service-cart/src/main/resources/sql/cart.sql
unimall-service-order/src/main/resources/sql/order.sql
unimall-service-seckill/src/main/resources/sql/seckill.sql
unimall-service-comments/src/main/resources/sql/comment.sql
unimall-service-sendmsg/src/main/resources/sql/message.sql
unimall-service-admin/src/main/resources/sql/admin.sql
```

### 2. 启动 Nacos

```
cd <nacos-server-2.3.2目录>/bin
startup.cmd -m standalone
# 验证：浏览器访问 http://127.0.0.1:8848/nacos
```

### 3. 启动 Redis / ES（虚拟机）

- Redis：确认 192.168.89.101:6379 可连接，密码 Redis123456
- ES：确认 192.168.89.101:9200 可访问，**IK 和拼音插件已安装**（索引创建依赖）

## 三、前置：config 端激活 native 模式（必须）

所有服务启动时都从 config 拉配置，config 必须先能提供 `config-repo/` 下的文件。

编辑 `unimall-config/src/main/resources/application.yml`，追加：

```yaml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config-repo
```

（注：`config-repo/` 下 13 个配置文件已齐，含各服务 `{name}-dev.yml` 与共享 `application.yml`）

## 四、启动顺序

按依赖顺序逐个启动（IDEA 中 Run，或 `mvn -pl <模块> spring-boot:run`）：

```
1. unimall-config        (10010)  ← 先启动，其余服务依赖它
2. unimall-service-user  (10012)
   unimall-service-goods (10013)
   unimall-service-cart  (10014)
   unimall-service-order (10015)
   unimall-service-seckill (10016)
   unimall-service-comments (10017)
   unimall-service-upload (10018)
   unimall-service-sendmsg (10019)
   unimall-service-search (10020)  ← 依赖 ES 可连接
   unimall-service-admin (10021)
3. unimall-gateway       (10011)  ← 最后启动，作为统一入口
```

启动成功的标志：日志无报错、`/actuator/health` 可用（如配了 actuator）、Nacos 控制台服务列表能看到注册的服务。

## 五、功能验证（curl 命令，网关 10011）

> Windows Git Bash 下可用。`TOKEN` 变量用登录返回值替换。

### 1. 用户：注册 / 登录 / 信息

```bash
# 注册（白名单，无需 token）
curl -X POST http://localhost:10011/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"tom123","password":"abc123456","nickname":"汤姆"}'

# 登录（返回 token）
curl -X POST http://localhost:10011/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tom123","password":"abc123456"}'
# 记录返回的 data.token，后续 TOKEN=...

# 用户信息（需 token）
curl http://localhost:10011/api/user/info -H "Authorization: Bearer $TOKEN"
```

### 2. 商品：分类 / 列表 / 详情

```bash
curl http://localhost:10011/api/category/list
curl "http://localhost:10011/api/goods/list?pageNum=1&pageSize=10"
curl http://localhost:10011/api/goods/detail/1
```

### 3. 购物车

```bash
curl -X POST http://localhost:10011/api/cart/add \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"goodsId":1,"quantity":2}'
curl http://localhost:10011/api/cart/list -H "Authorization: Bearer $TOKEN"
```

### 4. 订单：下单 / 支付 / 取消

```bash
# 下单（从购物车选中条目）
curl -X POST http://localhost:10011/api/order/create -H "Authorization: Bearer $TOKEN"
# 订单列表 / 详情
curl "http://localhost:10011/api/order/list?pageNum=1&pageSize=10" -H "Authorization: Bearer $TOKEN"
curl http://localhost:10011/api/order/detail/1 -H "Authorization: Bearer $TOKEN"
# 支付（0待付款 → 1已付款）
curl -X POST http://localhost:10011/api/order/pay/1 -H "Authorization: Bearer $TOKEN"
# 取消（恢复库存）
curl -X POST http://localhost:10011/api/order/cancel/2 -H "Authorization: Bearer $TOKEN"
```

### 5. 秒杀

```bash
# 创建活动（管理接口，admin token）
curl -X POST http://localhost:10011/api/seckill/activity \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"goodsId":1,"goodsName":"测试商品","seckillPrice":0.01,"stock":100,"limitPerUser":1,"startTime":"2026-01-01T00:00:00","endTime":"2027-01-01T00:00:00"}'
# 活动列表
curl http://localhost:10011/api/seckill/list
# 抢购（并发验证防超卖：可多终端同时执行）
curl -X POST http://localhost:10011/api/seckill/1 -H "Authorization: Bearer $TOKEN"
# 结果查询
curl http://localhost:10011/api/seckill/result/<orderNo> -H "Authorization: Bearer $TOKEN"
```

**防超卖验证**：活动库存 100，用脚本并发 200 次抢购（如 `for i in {1..200}; do curl ... & done`），统计成功数应 ≤ 100，且不出现"超卖"（Redis Lua 原子扣减保证）。

### 6. 评论

```bash
curl -X POST http://localhost:10011/api/comment \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"goodsId":1,"content":"很好用","rating":5}'
curl "http://localhost:10011/api/comment/list?goodsId=1"
```

### 7. 上传

```bash
# 上传图片（需 token；返回 /api/upload/{文件名}，可直接访问）
curl -X POST http://localhost:10011/api/upload \
  -H "Authorization: Bearer $TOKEN" -F "file=@/path/to/test.jpg"
# 访问上传的图片（公开）
curl -o /dev/null -w "%{http_code}" http://localhost:10011/api/upload/xxx.jpg
```

### 8. 短信 / 站内信

```bash
# 发验证码（白名单）
curl -X POST http://localhost:10011/api/sms/send \
  -H "Content-Type: application/json" -d '{"phone":"13800138000"}'
# 校验（验证码在 sendmsg 服务日志中查看）
curl -X POST http://localhost:10011/api/sms/verify \
  -H "Content-Type: application/json" -d '{"phone":"13800138000","code":"<日志中的验证码>"}'
# 站内信
curl -X POST http://localhost:10011/api/message/send \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"userId":1,"title":"欢迎","content":"欢迎注册"}'
curl http://localhost:10011/api/message/list -H "Authorization: Bearer $TOKEN"
curl http://localhost:10011/api/message/unread-count -H "Authorization: Bearer $TOKEN"
```

### 9. 搜索（ES）

```bash
# 手动触发全量同步（admin token；或等定时 10 分钟自动同步）
curl -X POST http://localhost:10011/api/search/sync -H "Authorization: Bearer $ADMIN_TOKEN"
# 搜索（中文关键词 / 拼音均可，验证 IK + 拼音分词）
curl "http://localhost:10011/api/search/goods?keyword=手机"
curl "http://localhost:10011/api/search/goods?keyword=shouji"
```

### 10. 后台管理

```bash
# 管理员登录（初始账号 admin/123456，服务启动时自动创建）
curl -X POST http://localhost:10011/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# 记录 ADMIN_TOKEN=...

# 商品管理
curl "http://localhost:10011/api/admin/goods/list?pageNum=1&pageSize=10" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -X PUT http://localhost:10011/api/admin/goods/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":1,"status":1}'

# 订单管理（列表/发货/取消）
curl "http://localhost:10011/api/admin/order/list?status=1" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -X PUT http://localhost:10011/api/admin/order/ship/1 -H "Authorization: Bearer $ADMIN_TOKEN"

# 用户管理（列表/禁用）
curl "http://localhost:10011/api/admin/user/list" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -X PUT http://localhost:10011/api/admin/user/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":2,"status":0}'

# 秒杀活动管理
curl http://localhost:10011/api/admin/seckill/list -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 六、鉴权验证（网关）

| 场景 | 期望 |
|---|---|
| 无 token 访问 `/api/user/info` | 401 `{"code":401,"message":"未登录或token已失效"}` |
| 伪造 token | 401 |
| 白名单接口（登录/注册/商品浏览/评论列表/搜索/上传资源/发验证码） | 无需 token，正常返回 |
| 管理面 `/api/admin/*`（除 login）无 token | 401（admin 内部拦截器） |
| 用户禁用后登录 | `1004 账号已被禁用` |

## 七、常见问题排查

| 现象 | 原因 | 处理 |
|---|---|---|
| 服务启动报"config 拉取失败/404" | config 未激活 native 或 `{name}-dev.yml` 缺失 | 按第三节配置 native；检查 config-repo 文件齐全 |
| 网关 503（路由到服务失败） | 目标服务未注册到 Nacos | 先启动业务服务，Nacos 控制台确认注册 |
| 购物车/订单接口 500 | goods 服务不可用（Feign 调用失败） | 确认 goods 已启动；后续可加 Feign 熔断 |
| 秒杀抢购全部返回"已被抢光" | 活动时间未到/已过，或 Redis 中库存为 0 | 检查活动时间；`redis-cli GET seckill:stock:{id}` 确认 |
| 搜索报错/搜索无结果 | ES 未启动 / 未同步数据 / 分词器插件缺失 | 启动 ES；`POST /api/search/sync`；确认 IK/拼音插件 |
| 上传 500 | 存储目录不可写（默认 `D:/unimall-upload/`） | 确认目录存在可写，或改 `unimall.upload.path` |
| 管理员登录失败 | 初始账号未创建（admin_user 表非空时会跳过） | 删空 admin_user 表重启，或手动插入 BCrypt 密码 |
| 图片 404 | 上传目录与访问映射不一致 | 确认 `unimall.upload.path` 尾斜杠、与 `UploadConfig` 映射一致 |


---

## 八、测试进度记录（2026-08-06 首次验证）

### ✅ 已验证通过

| 项 | 结果 |
|---|---|
| 环境 | MySQL（已建表）/ Nacos 2.3.1 / Redis（虚拟机）/ ES 8.19.17（IK+拼音）/ RabbitMQ 3-management 全部启动 |
| config | git 模式（gitee 私有仓库 + `GITEE_TOKEN` 环境变量），`user-dev.yml` 返回 datasource + jwt（search-paths 生效） |
| 服务启动 | user / admin / gateway 及全部 11 个服务启动成功，Nacos 注册正常 |
| 注册 | `POST /api/user/register` → `data:1`（新用户 id）✅ |
| 登录 | `POST /api/user/login` → 返回 JWT（HS512）+ expiresIn 1800 ✅ |
| 鉴权 | `GET /api/user/info`（带 token）→ 返回用户信息 ✅（token 必须单行无换行） |
| 商品 | 新增（默认下架）→ 上架 → 列表（status=1）✅ |
| 购物车 | 加购（Feign 校验商品）→ 列表（Feign 批量查商品，含小计）✅ |
| 订单 | 下单（扣库存→建订单快照→清购物车）→ 列表/详情（明细快照正确）✅ |
| admin | 初始管理员 `admin/123456` 自动创建（AdminInitializer）✅ |
| 订单支付/取消 | 支付（0→1+payTime）✅；取消（库存回补 98→99）✅ |
| 购物车冲突修复 | 下单清购物车改**物理删除**（逻辑删除占用唯一索引导致"删了再加购"冲突）→ 再加购成功 ✅ |
| 秒杀 | Lua 防超卖：库存 100 并发 200 → **恰好 100 条订单**（无超卖）✅；**联动扣减 goods 库存**（200→100）✅；抢购/结果查询 ✅ |
| 权限修正 | 网关 `AdminProtectFilter`（order=-200）：普通用户访问管理操作/内部接口（`POST /api/goods`、`PUT /api/goods/status`、`/api/*/internal/**`、`/api/goods/batch|deduct|restore`、`POST /api/seckill/activity`）→ 403；抢购保留；admin Feign 直连不受影响 ✅ |
| 评论 | 发表（`code:0`，路由修复后）✅；列表（公开）✅ |
| 上传 | Apifox form-data（字段 `file`）上传成功，返回 `/api/upload/xxx.png` ✅ |
| 短信/站内信 | 发码（日志打印 118147）→ 校验 ✅；站内信发/列表/未读数/已读 ✅（路由补 sendmsg 后） |
| 搜索（ES） | 中文"华为" ✅ + 拼音"huawei"/"mate" ✅（IK+拼音分词）；修复 createTime Long 转换 + 补 stock 字段 |
| 后台管理 | admin 登录/商品列表/订单列表/用户列表/秒杀列表/上下架/发货/禁用 ✅（修复 user 缺分页插件导致 total=0） |

### ⏳ 待验证

- ~~Bus 动态刷新~~ → **已验证 ✅**：改配置 → push gitee → `POST /actuator/busrefresh`（204）→ 各服务热刷新（test-route 不重启 gateway 即生效）
- ~~Token 续期机制~~ → **已实现 + 验证 ✅**：滑动续期（JWT 7 天 `expire-seconds: 604800` + 会话 30 分钟 `session-seconds: 1800`），网关校验通过续 TTL（1735 → 请求后 1799），用户活跃不掉线

### 剩余 WIP

- 单元测试（项目无测试框架）
- 生产化项（Feign 熔断、RBAC、统计概览、限流等）

### 🕳 测试踩过的坑（排障参考）

1. **config 只返回 jwt（业务配置缺失）**：gitee 仓库业务文件放在 `{name}-dev/` 子目录 → config 加 `search-paths: '*-dev'` 解决（application.yml 在根目录不受影响）
2. **user 启动失败 `JwtUtil bean not found`**：user 模块缺 `JwtUtil` Bean 定义 → 补 `config/JwtConfig.java`
3. **admin 启动循环依赖**（`WebConfig` 定义 JwtUtil Bean + 构造器依赖拦截器形成三角循环）→ JwtUtil Bean 移到独立 `JwtConfig`
4. **鉴权 401**：token 复制时折行（curl 里换行破坏 token）→ 必须单行完整 token
5. **加购 3001 商品不存在或已下架**：新增商品**默认下架**（status=0），需先 `PUT /goods/status` 上架
6. **命令行单独编译报找不到 common 类**：本地仓库 `unimall-common` jar 过旧 → `mvn -pl unimall-common install` 或 `-am` 编译
7. **HTTP 方法用错**：加购是 POST、列表是 GET，注意区分（405/5000 排查先看方法）
8. **评论 404**：网关路由 `Path=/api/comments/**`（复数）与服务端 `/comment`（单数）不一致 → 路由改 `/api/comment/**`
9. **权限漏洞**：商品新增/上下架、秒杀建活动、各服务 `/internal/**` 接口暴露给普通用户 → 网关加 `AdminProtectFilter` 黑名单（403）
10. **短信 404**：sendmsg 无网关路由 → gateway-dev.yml 补 `/api/sms/**`、`/api/message/**` 路由
11. **搜索报错（Long→LocalDateTime）**：ES 的 createTime 存成 epoch_millis Long，Spring Data ES 读不回 LocalDateTime → GoodsDoc.createTime 改 Long + 手动转换 + mapping `epoch_millis`（需删索引重建）
12. **用户管理 total=0**：user 模块缺分页插件（MybatisPlusConfig）→ 补上（其他模块都有，user 漏了）
13. **RabbitMQ ACCESS_REFUSED**：guest 默认禁止远程 → 建用户 `user/123456`（虚拟机 rabbitmqctl）改配置
14. **config 服务 busrefresh 405/未暴露**：config 是 Server 不拉 gitee 共享配置 → management/rabbitmq 配置必须放 **config 本地 application.yml**
15. **未知路径返回 5000**：Spring Boot 3.2+ `NoResourceFoundException` 被 `@ExceptionHandler(Exception)` 兜底 → 各服务 GlobalExceptionHandler 单独处理返回 404
16. **busrefresh 返回 204**：动作型端点正常响应（成功无内容），非错误

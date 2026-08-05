# unimall-service-sendmsg 消息模块说明

> 消息服务：短信验证码（模拟）+ 站内信通知。

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

消息服务（端口 **10019**）：

- **短信验证码**（模拟发送）：发码存 Redis（TTL 5 分钟）+ 一次性校验
- **站内信**：系统发消息给用户、我的消息、未读数、标记已读

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / MyBatis-Plus 3.5.5 / MySQL 8 / Redis（验证码存储）。

## 三、数据库设计

建表脚本：`src/main/resources/sql/message.sql`

**message**：`id / user_id / title(≤100) / content(≤1000) / is_read(0未读 1已读) / 通用字段`

> 短信验证码**不落库**（Redis：`sms:code:{phone}`，TTL 300s，一次性）。

## 四、接口清单

### 短信验证码

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /sms/send` | 发 6 位验证码 → Redis（TTL 300s）→ 日志模拟发送 | **白名单**（`/api/sms/send`） |
| `POST /sms/verify` | 校验（错误 8002 / 过期 8003），**一次性**：通过即删 key | 需登录 |

### 站内信

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /message/send` | 系统发站内信（指定 userId） | 需登录 |
| `GET /message/list` | 我的消息分页（时间倒序） | 需登录 |
| `GET /message/unread-count` | 未读数 | 需登录 |
| `PUT /message/read/{id}` | 标记已读（只能标自己的） | 需登录 |

错误码：`8002 验证码错误` / `8003 验证码已过期` / `8004 消息不存在` / `1005` / `5000`。

## 五、功能实现思路

- **发验证码**：`String.format("%06d", random)` → `set(sms:code:{phone}, code, 300s)` → `log.info` 模拟发送（未接真实短信商）
- **校验**：`get` → null 抛 8003；不匹配 8002；匹配 → `delete`（一次性）
- **站内信**：`send` 入库（is_read=0）；`page` 按 userId 分页；`unreadCount` 统计；`markRead` 归属校验后置 1

## 六、目录结构

```
com.unimall.sendmsg/
├── SendmsgApplication.java        # @SpringBootApplication + @MapperScan
├── controller/SmsController, MessageController
├── service/ISmsService, IMessageService + impl/
├── mapper/IMessageMapper.java
├── pojo/entity/Message.java、dto/SmsSendDTO, SmsVerifyDTO, MessageSendDTO、vo/MessageVO
└── config/GlobalExceptionHandler, MybatisPlusConfig（分页）
```

## 七、配置说明

- 本地 `application.yml`：端口 10019、Nacos、config 客户端
- 配置中心 `config-repo/sendmsg-dev.yml`：datasource + Redis（虚拟机）+ MyBatis-Plus
- 手机号正则：`^1[3-9]\d{9}$`

## 八、启动前提与已知问题

- 前提：MySQL 建表、Nacos、Redis、config native 激活
- 已知问题：验证码接口未接入 user 注册/登录流程（手机号注册可后续接 `/sms/verify`）；未做发送频率限制（60s 内不重复）；站内信发送未走消息总线/异步

# unimall-service-sendmsg — 消息服务

消息服务（端口 **10019**）：短信验证码（模拟）+ 站内信通知。

## 接口

### 短信验证码（模拟发送，不接真实短信商）

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /sms/send` | 发 6 位验证码 → 存 Redis `sms:code:{phone}`（TTL 300s）→ 日志模拟发送 | **公开**（白名单 `/api/sms/send`） |
| `POST /sms/verify` | 校验（错误 8002 / 过期 8003），**一次性**：通过即删 key | 需登录 |

### 站内信

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /message/send` | 发送站内信（管理/系统调用，指定 userId） | 需登录 |
| `GET /message/list` | 我的消息分页（时间倒序） | 需登录 |
| `GET /message/unread-count` | 未读数 | 需登录 |
| `PUT /message/read/{id}` | 标记已读（只能标自己的） | 需登录 |

## 分层与命名（与 user/goods 一致）

```
com.unimall.sendmsg/
├── SendmsgApplication.java     # @SpringBootApplication + @MapperScan
├── controller/SmsController.java, MessageController.java
├── service/ISmsService, IMessageService + impl/
├── mapper/IMessageMapper.java
├── pojo/entity/Message.java
├── pojo/dto/SmsSendDTO, SmsVerifyDTO, MessageSendDTO
├── pojo/vo/MessageVO.java
└── config/GlobalExceptionHandler.java + MybatisPlusConfig.java
```

## 要点

- 错误码：`8002 验证码错误` / `8003 验证码已过期` / `8004 消息不存在` / `1005` / `5000`
- 短信验证码**不落库**（Redis：`sms:code:{phone}`，TTL 300s，一次性）；手机号正则 `^1[3-9]\d{9}$`
- 站内信表 `message`（userId/title/content/is_read）；短信发送是模拟（`log.info` 打印验证码），生产替换为短信商 SDK
- Redis 用 `StringRedisTemplate`（MVC 服务）
- 业务配置在配置中心 `sendmsg-dev.yml`（datasource + redis + mp）
- MyBatis-Plus 3.5.5（勿升回 3.5.17）

## 已知问题/后续

- 验证码接口暂未接入 user 注册/登录流程（手机号注册可后续接：注册时校验 `/sms/verify`）
- 未做发送频率限制（如 60 秒内不重复发送）
- 站内信发送未接消息总线/异步（直接入库）

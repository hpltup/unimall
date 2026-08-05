# unimall-service-seckill — 秒杀服务

秒杀服务（端口 **10016**）：秒杀活动管理 + Redis Lua 原子防超卖抢购。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /seckill/activity` | 创建秒杀活动（活动表冗余商品快照，不依赖 goods 服务） | 需登录（管理） |
| `GET /seckill/list` | 活动列表（未开始 + 进行中，按开始时间升序） | 白名单可配 |
| `GET /seckill/detail/{id}` | 活动详情 | 白名单可配 |
| `POST /seckill/{activityId}` | **抢购**：时间校验 → 懒加载预热 → Lua 原子扣库存+限购 → 建秒杀订单 → 返回订单号 | 需登录 |
| `GET /seckill/result/{orderNo}` | 查询秒杀结果（同步方案抢购成功即存在；MQ 演进后前端轮询此接口） | 需登录 |

## 防超卖核心（Redis Lua）

```lua
-- KEYS[1]=库存key KEYS[2]=限购key ARGV[1]=限购数
local stock  = tonumber(redis.call('GET', KEYS[1]) or '0')
local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
if stock <= 0 then return -1 end        -- 已抢光
if bought >= tonumber(ARGV[1]) then return -2 end  -- 超限购
redis.call('DECR', KEYS[1])
redis.call('INCR', KEYS[2])
return 1
```

- **Redis 单线程 + Lua 原子执行**：扣库存与限购校验一气呵成，并发下不可能超卖
- key 设计：`seckill:stock:{activityId}`、`seckill:limit:{activityId}:{userId}`
- **懒加载预热**：抢购时库存 key 不存在则从 DB 写入（`setIfAbsent` 幂等），无需单独预热接口

## 分层与命名

```
com.unimall.seckill/
├── SeckillApplication.java       # @SpringBootApplication + @MapperScan
├── controller/                   # /seckill/*
├── service/ISeckillService.java + impl/SeckillServiceImpl.java
├── mapper/ISeckillActivityMapper.java + ISeckillOrderMapper.java
├── pojo/entity/SeckillActivity.java, SeckillOrder.java
├── pojo/dto/SeckillActivityCreateDTO.java
├── pojo/vo/SeckillActivityVO.java, SeckillOrderVO.java
└── config/GlobalExceptionHandler.java
```

## 要点

- 错误码：`5001 活动不存在` / `5002 未开始或已结束` / `5003 已被抢光` / `5004 超过限购` / `5005 暂无秒杀结果` / `1005` / `5000`
- **秒杀订单独立**（`seckill_order` 表，含商品快照），不走 order 服务——链路最短
- 状态字段：活动 `0未开始 1进行中 2已结束`（**按时间实时计算**，DB status 为冗余）；订单 `0待付款 1已付款 2已完成 3已取消`
- Redis 用 **`StringRedisTemplate`**（MVC 服务）；配置在配置中心 `seckill-dev.yml`（含 Redis 连接）
- **MQ 演进预留**：`createSeckillOrder(activity, userId)` 独立方法 + `result` 查询接口——将来切 RabbitMQ 异步削峰时，把 `doSeckill` 里的同步调用换成发消息 + `@RabbitListener` 消费者调用建单方法即可，Redis 逻辑零改动
- 限流未做（学习项目，Redis 库存天然控流；后续可在网关接 Sentinel）
- MyBatis-Plus 3.5.5（勿升回 3.5.17）

## 已知问题

- 活动创建/编辑后无独立的预热接口（懒加载预热已覆盖首次抢购）
- 秒杀库存与 goods 服务库存**相互独立**（活动库存单独管理），未联动扣减 goods 库存

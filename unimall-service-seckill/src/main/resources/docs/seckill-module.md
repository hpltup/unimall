# unimall-service-seckill 秒杀模块说明

> 秒杀服务：秒杀活动管理 + **Redis Lua 原子防超卖**抢购，独立秒杀订单，预留 MQ 异步演进。

## 目录

- [一、模块定位](#一模块定位)
- [二、技术栈](#二技术栈)
- [三、数据库设计](#三数据库设计)
- [四、接口清单](#四接口清单)
- [五、防超卖核心（Redis Lua）](#五防超卖核心redis-lua)
- [六、抢购流程](#六抢购流程)
- [七、MQ 演进预留](#七mq-演进预留)
- [八、目录结构](#八目录结构)
- [九、配置说明](#九配置说明)
- [十、启动前提与已知问题](#十启动前提与已知问题)

---

## 一、模块定位

秒杀服务（端口 **10016**）：

- 秒杀活动创建 / 列表 / 详情（活动表冗余商品快照，不依赖 goods 服务）
- 抢购：**Redis Lua 原子扣库存 + 限购计数** → 建秒杀订单（同步）
- 结果查询（MQ 演进后由前端轮询）

## 二、技术栈

Spring Boot 3.3.7 / Spring Cloud 2023.0.1 / Nacos / Config / MyBatis-Plus 3.5.5 / MySQL 8 / **Redis（StringRedisTemplate + Lua 脚本）**。

不引入 MQ / Redisson / Sentinel（保持简化，演进路径见第七节）。

## 三、数据库设计

建表脚本：`src/main/resources/sql/seckill.sql`

**seckill_activity**：`id / goods_id / goods_name(快照) / goods_image / seckill_price / stock / limit_per_user / start_time / end_time / status(冗余) / 通用字段`
**seckill_order**：`id / order_no(唯一) / activity_id / user_id / goods_id / goods_name(快照) / seckill_price / quantity / total / status(0待付款 1已付款 2已完成 3已取消) / 通用字段`

## 四、接口清单

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /seckill/activity` | 创建活动（管理） | 需登录 |
| `GET /seckill/list` | 活动列表（未开始+进行中，按开始时间升序） | 可配白名单 |
| `GET /seckill/detail/{id}` | 活动详情（status 按当前时间实时计算） | 可配白名单 |
| `POST /seckill/{activityId}` | **抢购**：Lua 扣库存+限购 → 建订单 → 返回 orderNo | 需登录 |
| `GET /seckill/result/{orderNo}` | 查询秒杀结果 | 需登录 |

错误码：`5001 活动不存在` / `5002 未开始或已结束` / `5003 已被抢光` / `5004 超过限购` / `5005 暂无秒杀结果` / `1005` / `5000`。

## 五、防超卖核心（Redis Lua）

```lua
-- KEYS[1]=库存key(seckill:stock:{activityId})  KEYS[2]=限购key(seckill:limit:{activityId}:{userId})
-- ARGV[1]=限购数量
local stock  = tonumber(redis.call('GET', KEYS[1]) or '0')
local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
if stock <= 0 then return -1 end                          -- 已抢光
if bought >= tonumber(ARGV[1]) then return -2 end         -- 超限购
redis.call('DECR', KEYS[1])
redis.call('INCR', KEYS[2])
return 1
```

**Redis 单线程 + Lua 原子执行**：扣库存与限购校验一气呵成，并发下不可能超卖。

**懒加载预热**：抢购时库存 key 不存在则从 DB 写入（`setIfAbsent` 幂等），无需单独预热接口。

## 六、抢购流程

```
POST /seckill/{activityId}（X-User-Id）
  1. 查活动（空 5001）
  2. 时间校验（未开始/已结束 5002）
  3. 懒加载预热库存 key
  4. Lua 执行：-1 → 5003 抢光；-2 → 5004 超限购；1 → 继续
  5. createSeckillOrder(activity, userId) 建秒杀订单（快照）→ 返回 orderNo
```

## 七、MQ 演进预留

- `createSeckillOrder(activity, userId)` **独立方法**——将来切 RabbitMQ 异步削峰时，由 `@RabbitListener` 消费者调用
- `GET /seckill/result/{orderNo}` 轮询接口已就绪——异步化后前端改轮询即可，**Redis 逻辑零改动**

## 八、目录结构

```
com.unimall.seckill/
├── SeckillApplication.java        # @SpringBootApplication + @MapperScan
├── controller/SeckillController.java
├── service/ISeckillService.java + impl/SeckillServiceImpl.java（含 Lua 脚本）
├── mapper/ISeckillActivityMapper, ISeckillOrderMapper
├── pojo/entity/SeckillActivity, SeckillOrder、pojo/vo/SeckillOrderVO
└── config/GlobalExceptionHandler.java
```

跨服务契约：`SeckillActivityCreateDTO`（common.dto）、`SeckillActivityVO`（common.vo）。

## 九、配置说明

- 本地 `application.yml`：端口 10016、Nacos、config 客户端
- 配置中心 `config-repo/seckill-dev.yml`：datasource + **Redis（虚拟机 192.168.89.101）** + MyBatis-Plus

## 十、启动前提与已知问题

- 前提：MySQL 建表、Nacos、**Redis 虚拟机**、config native 激活
- 已知问题：活动创建/编辑后无独立预热接口（懒加载已覆盖首次抢购）；秒杀库存与 goods 库存**相互独立**（未联动扣减）；限流未做（后续可在网关接 Sentinel）

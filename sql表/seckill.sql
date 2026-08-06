-- ============================================================
-- unimall 商城系统 - 秒杀模块
-- 设计要点：
--  1. seckill_activity：秒杀活动，冗余商品快照（名称/图/秒杀价），不依赖 goods 服务实时数据
--  2. seckill_order：秒杀订单（独立于 order 服务，链路最短）
--  3. 防超卖：活动库存预热到 Redis（seckill:stock:{id}），抢购用 Lua 原子扣减
--  4. 限购：Redis 计数 seckill:limit:{activityId}:{userId}，Lua 内原子校验
--  5. 状态：0未开始 1进行中 2已结束（时间驱动，status 为冗余字段）
-- ============================================================

USE `unimall`;

-- 秒杀活动表
DROP TABLE IF EXISTS `seckill_activity`;

CREATE TABLE `seckill_activity`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    `goods_id`       BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `goods_name`     VARCHAR(100)    NOT NULL COMMENT '商品名称（快照）',
    `goods_image`    VARCHAR(255)    DEFAULT NULL COMMENT '商品主图（快照）',
    `seckill_price`  DECIMAL(10, 2)  NOT NULL COMMENT '秒杀价（元）',
    `stock`          INT             NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    `limit_per_user` INT             NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `start_time`     DATETIME        NOT NULL COMMENT '开始时间',
    `end_time`       DATETIME        NOT NULL COMMENT '结束时间',
    `status`         TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0未开始 1进行中 2已结束（冗余，实际按时间判断）',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='秒杀活动表';

-- 秒杀订单表
DROP TABLE IF EXISTS `seckill_order`;

CREATE TABLE `seckill_order`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`      VARCHAR(32)     NOT NULL COMMENT '订单号',
    `activity_id`   BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `goods_id`      BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `goods_name`    VARCHAR(100)    NOT NULL COMMENT '商品名称（快照）',
    `goods_image`   VARCHAR(255)    DEFAULT NULL COMMENT '商品主图（快照）',
    `seckill_price` DECIMAL(10, 2)  NOT NULL COMMENT '秒杀价（快照）',
    `quantity`      INT             NOT NULL DEFAULT 1 COMMENT '数量',
    `total`         DECIMAL(12, 2)  NOT NULL COMMENT '总价 = 秒杀价 * 数量',
    `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0待付款 1已付款 2已完成 3已取消',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_activity_user` (`activity_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='秒杀订单表';

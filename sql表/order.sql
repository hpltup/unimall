-- ============================================================
-- unimall 商城系统 - 订单模块
-- 设计要点：
--  1. 表名用 orders（order 是 MySQL 保留字）
--  2. 订单状态 status：0待付款 1已付款 2已完成 3已取消
--  3. order_item 存商品快照（名称/图/价格），不依赖商品服务的实时数据
--  4. 下单流程：扣库存(goods) → 建订单(本地事务) → 清购物车(cart)
-- ============================================================

USE `unimall`;

-- 订单主表
DROP TABLE IF EXISTS `orders`;

CREATE TABLE `orders`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`    VARCHAR(32)     NOT NULL COMMENT '订单号',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额（元）',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0待付款 1已付款 2已完成 3已取消',
    `pay_time`    DATETIME        DEFAULT NULL COMMENT '支付时间',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='订单主表';

-- 订单明细表（商品快照）
DROP TABLE IF EXISTS `order_item`;

CREATE TABLE `order_item`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    `goods_id`    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `goods_name`  VARCHAR(100)    NOT NULL COMMENT '商品名称（快照）',
    `goods_image` VARCHAR(255)    DEFAULT NULL COMMENT '商品主图（快照）',
    `price`       DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '成交单价（快照）',
    `quantity`    INT             NOT NULL DEFAULT 1 COMMENT '数量',
    `total`       DECIMAL(12, 2)  NOT NULL DEFAULT 0.00 COMMENT '小计 = price * quantity',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='订单明细表';

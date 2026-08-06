-- ============================================================
-- unimall 商城系统 - 购物车表
-- 设计要点：
--  1. 按用户维度存储（user_id），通过网关 X-User-Id 获取当前用户
--  2. (user_id, goods_id) 唯一：同一商品重复添加合并数量
--  3. checked 选中标记：下单时只处理选中的条目
--  4. 商品信息（名称/图/价格）不入库，列表展示时经 OpenFeign 查 goods 服务
-- ============================================================

USE `unimall`;

DROP TABLE IF EXISTS `cart`;

CREATE TABLE `cart`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车条目ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `goods_id`    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `quantity`    INT             NOT NULL DEFAULT 1 COMMENT '数量',
    `checked`     TINYINT         NOT NULL DEFAULT 1 COMMENT '是否选中：0否 1是',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='购物车表';

DELETE FROM cart WHERE deleted = 1;
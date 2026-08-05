-- ============================================================
-- unimall 商城系统 - 商品评论表
-- 设计要点：
--  1. 评论挂在商品下（goods_id），用户发表（user_id，来自网关 X-User-Id）
--  2. rating 评分 1~5（后续可聚合商品平均分）
--  3. status：0待审核 1显示 2隐藏（简化：默认 1 直接显示）
--  4. images 图片 JSON 数组存储（["url1","url2"]）
-- ============================================================

USE `unimall`;

DROP TABLE IF EXISTS `comment`;

CREATE TABLE `comment`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `goods_id`    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `content`     VARCHAR(500)    NOT NULL COMMENT '评论内容',
    `images`      TEXT            DEFAULT NULL COMMENT '图片JSON数组 ["url1","url2"]',
    `rating`      TINYINT         NOT NULL DEFAULT 5 COMMENT '评分：1~5',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0待审核 1显示 2隐藏',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品评论表';

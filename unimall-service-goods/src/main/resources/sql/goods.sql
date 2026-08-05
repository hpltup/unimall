-- ============================================================
-- unimall 商城系统 - 商品模块（简化版：无 SPU/SKU 拆分）
-- 设计要点：
--  1. category 分类表：支持两级分类（parent_id），商城常见
--  2. goods 商品表：价格/库存/销量直接放商品表，上下架 status 控制
--  3. images 轮播图 JSON 数组存储（["url1","url2"]）
--  4. deleted 逻辑删除 + 时间字段数据库默认值（MyBatis-Plus 约定）
-- ============================================================

USE `unimall`;

-- 商品分类表
DROP TABLE IF EXISTS `category`;

CREATE TABLE `category`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(32)     NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID（0 为一级分类）',
    `sort`        INT             NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品分类表';

-- 商品表
DROP TABLE IF EXISTS `goods`;

CREATE TABLE `goods`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `category_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    `name`        VARCHAR(100)    NOT NULL COMMENT '商品名称',
    `sub_title`   VARCHAR(255)    DEFAULT NULL COMMENT '副标题/卖点',
    `main_image`  VARCHAR(255)    DEFAULT NULL COMMENT '主图URL',
    `images`      TEXT            DEFAULT NULL COMMENT '轮播图JSON数组 ["url1","url2"]',
    `detail`      LONGTEXT        DEFAULT NULL COMMENT '商品详情（富文本）',
    `price`       DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '售价（元）',
    `market_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '市场价（划线价）',
    `stock`       INT             NOT NULL DEFAULT 0 COMMENT '库存',
    `sales`       INT             NOT NULL DEFAULT 0 COMMENT '销量',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0下架 1上架',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品表';

-- 初始分类数据
INSERT INTO `category` (`name`, `parent_id`, `sort`) VALUES ('手机数码', 0, 1);
INSERT INTO `category` (`name`, `parent_id`, `sort`) VALUES ('服饰鞋包', 0, 2);
INSERT INTO `category` (`name`, `parent_id`, `sort`) VALUES ('食品生鲜', 0, 3);

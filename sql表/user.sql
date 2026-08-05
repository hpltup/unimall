-- ============================================================
-- unimall 商城系统 - 用户表
-- 设计要点：
--  1. 单表设计：账号 + 基础资料 + 资产（余额/积分）+ 状态，满足商城常用场景
--  2. username / phone / email 均可作为登录凭据，各建唯一索引
--  3. id_card 实名认证字段按脱敏存储
--  4. deleted 逻辑删除（配合 MyBatis-Plus @TableLogic）
--  5. create_time / update_time 数据库默认值 + 自动更新
-- ============================================================

CREATE DATABASE IF NOT EXISTS `unimall`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `unimall`;

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`         VARCHAR(32)     NOT NULL COMMENT '用户名（唯一，登录凭据）',
    `password`         VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt 加密后存储）',
    `nickname`         VARCHAR(32)     DEFAULT NULL COMMENT '昵称',
    `phone`            VARCHAR(20)     DEFAULT NULL COMMENT '手机号（唯一，可作登录凭据）',
    `email`            VARCHAR(64)     DEFAULT NULL COMMENT '邮箱（唯一，可作登录凭据）',
    `avatar`           VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
    `gender`           TINYINT         NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `birthday`         DATE            DEFAULT NULL COMMENT '生日',
    `real_name`        VARCHAR(32)     DEFAULT NULL COMMENT '真实姓名（实名认证）',
    `id_card`          VARCHAR(32)     DEFAULT NULL COMMENT '身份证号（实名认证，脱敏存储）',
    `balance`          DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '账户余额（元）',
    `points`           INT             NOT NULL DEFAULT 0 COMMENT '积分',
    `level`            TINYINT         NOT NULL DEFAULT 1 COMMENT '会员等级：1普通 2白银 3黄金 4钻石',
    `status`           TINYINT         NOT NULL DEFAULT 1 COMMENT '账号状态：0禁用 1正常',
    `last_login_time`  DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`    VARCHAR(45)     DEFAULT NULL COMMENT '最后登录IP（VARCHAR(45) 兼容IPv6）',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

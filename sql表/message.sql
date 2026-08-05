-- ============================================================
-- unimall 商城系统 - 站内信表
-- 短信验证码不落库（存 Redis：sms:code:{phone}，TTL 5 分钟）
-- ============================================================

USE `unimall`;

DROP TABLE IF EXISTS `message`;

CREATE TABLE `message`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '收件人用户ID',
    `title`       VARCHAR(100)    NOT NULL COMMENT '标题',
    `content`     VARCHAR(1000)   NOT NULL COMMENT '内容',
    `is_read`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='站内信表';

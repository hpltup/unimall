-- ============================================================
-- unimall 商城系统 - 管理员表
-- 初始账号（admin / 123456）由 admin 服务启动时自动创建（CommandLineRunner，BCrypt 加密）
-- ============================================================

USE `unimall`;

DROP TABLE IF EXISTS `admin_user`;

CREATE TABLE `admin_user`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username`    VARCHAR(32)     NOT NULL COMMENT '用户名（唯一）',
    `password`    VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt 加密）',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='管理员表';

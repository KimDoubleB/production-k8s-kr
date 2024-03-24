GRANT ALL PRIVILEGES ON *.* TO 'user'@'%';

DROP DATABASE IF EXISTS `feature-flag`;

CREATE DATABASE `feature-flag` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

DROP TABLE IF EXISTS `feature-flag`.`todo`;

CREATE TABLE `feature-flag`.`todo` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `content` VARCHAR(256) NOT NULL,
    `completed` TINYINT NOT NULL DEFAULT 0,
    `created_date` DATETIME(6) NOT NULL DEFAULT NOW(6),
    `last_modified_date` DATETIME(6) NOT NULL DEFAULT NOW(6),
    PRIMARY KEY (`id`)
);
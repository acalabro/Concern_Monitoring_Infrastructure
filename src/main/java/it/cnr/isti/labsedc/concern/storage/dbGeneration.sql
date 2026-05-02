-- Riferimento rapido per sviluppatori.
-- Script completo e canonico: <project-root>/init-db.sql

CREATE DATABASE IF NOT EXISTS eventdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE eventdb;

CREATE TABLE IF NOT EXISTS `event` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `senderID`      VARCHAR(255)    NOT NULL,
  `timestamp`     BIGINT          NOT NULL,
  `data`          LONGBLOB        NOT NULL,
  `dataClassName` VARCHAR(512)    NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event_senderID`         (`senderID`),
  KEY `idx_event_timestamp`        (`timestamp`),
  KEY `idx_event_sender_timestamp` (`senderID`, `timestamp`),
  KEY `idx_event_dataClass`        (`dataClassName`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `violation` (
  `id`                         BIGINT NOT NULL AUTO_INCREMENT,
  `violationMessage`           TEXT            DEFAULT NULL,
  `probeNameThatTriggersError` VARCHAR(255)    DEFAULT NULL,
  `ruleViolatedName`           VARCHAR(255)    DEFAULT NULL,
  `violationTimestamp`         BIGINT          NOT NULL,
  `ruleMetadata`               VARCHAR(255)
      CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_violation_probe_ts` (`probeNameThatTriggersError`, `violationTimestamp`),
  KEY `idx_violation_rule_ts`  (`ruleViolatedName`,           `violationTimestamp`),
  KEY `idx_violation_ts`       (`violationTimestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `users` (
  `id`            BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(100)         NOT NULL,
  `password_hash` VARCHAR(255)         NOT NULL  COMMENT 'BCrypt',
  `role`          ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
  `created_at`    TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE OR REPLACE VIEW `v_violation_ts` AS
SELECT `id`, `violationMessage`, `probeNameThatTriggersError`,
       `ruleViolatedName`, `violationTimestamp`,
       FROM_UNIXTIME(`violationTimestamp`/1000) AS `violation_time`,
       `ruleMetadata`
FROM `violation`;

CREATE OR REPLACE VIEW `v_event_ts` AS
SELECT `id`, `senderID`, `timestamp`,
       FROM_UNIXTIME(`timestamp`/1000) AS `event_time`,
       `dataClassName`
FROM `event`;

CREATE USER IF NOT EXISTS 'concern'@'%' IDENTIFIED BY 'un53cur3!!';
GRANT ALL PRIVILEGES ON eventdb.* TO 'concern'@'%';

CREATE USER IF NOT EXISTS 'grafana'@'%' IDENTIFIED BY 'grafana123';
GRANT SELECT ON eventdb.* TO 'grafana'@'%';

FLUSH PRIVILEGES;

-- =============================================================================
-- Concern Monitoring Infrastructure — Database Init Script
-- =============================================================================
-- Uso diretto (MySQL CLI):
--   mysql -u root -p < init-db.sql
--
-- Nel container Docker l'inizializzazione avviene tramite
--   docker/init-db/init-eventdb.sh  (chiamato automaticamente da MySQL)
--
-- NOTA: le password degli utenti applicativi sono lette dall'env del backend.
--       Cambiarle qui richiede di aggiornare anche le variabili MYSQL_PASSWORD
--       e JWT_SECRET in docker-compose.yml / .env.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Database
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `eventdb`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `eventdb`;

-- ---------------------------------------------------------------------------
-- 2. Utente applicativo (concern)
-- ---------------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'concern'@'%' IDENTIFIED BY 'un53cur3!!';
GRANT ALL PRIVILEGES ON `eventdb`.* TO 'concern'@'%';

-- Utente read-only per Grafana / tool di osservabilità esterni (opzionale)
CREATE USER IF NOT EXISTS 'grafana'@'%' IDENTIFIED BY 'grafana123';
GRANT SELECT ON `eventdb`.* TO 'grafana'@'%';

FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------------
-- 3. Tabella: event
--    Raccoglie tutti gli eventi ricevuti dai probe.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `event` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `senderID`      VARCHAR(255)    NOT NULL,
  `timestamp`     BIGINT          NOT NULL  COMMENT 'Unix epoch ms',
  `data`          LONGBLOB        NOT NULL,
  `dataClassName` VARCHAR(512)    NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event_senderID`               (`senderID`),
  KEY `idx_event_timestamp`              (`timestamp`),
  KEY `idx_event_sender_timestamp`       (`senderID`, `timestamp`),
  KEY `idx_event_dataClass`              (`dataClassName`(64))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Raw events received from monitoring probes';

-- ---------------------------------------------------------------------------
-- 4. Tabella: violation
--    Registra ogni violazione di regola rilevata dal CEP.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `violation` (
  `id`                          BIGINT NOT NULL AUTO_INCREMENT,
  `violationMessage`            TEXT            DEFAULT NULL,
  `probeNameThatTriggersError`  VARCHAR(255)    DEFAULT NULL,
  `ruleViolatedName`            VARCHAR(255)    DEFAULT NULL,
  `violationTimestamp`          BIGINT          NOT NULL  COMMENT 'Unix epoch ms',
  `ruleMetadata`                VARCHAR(255)
      CHARACTER SET utf8mb4
      COLLATE utf8mb4_bin        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_violation_probe_ts`  (`probeNameThatTriggersError`, `violationTimestamp`),
  KEY `idx_violation_rule_ts`   (`ruleViolatedName`,           `violationTimestamp`),
  KEY `idx_violation_ts`        (`violationTimestamp`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Rule violations detected by the CEP engine';

-- ---------------------------------------------------------------------------
-- 5. Tabella: users
--    Utenti della dashboard web. Le password sono hash BCrypt (gestito da Java).
--    Gli account di default (admin/admin123, user/user123) vengono inseriti
--    automaticamente dal backend al primo avvio se la tabella è vuota.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(100)     NOT NULL,
  `password_hash` VARCHAR(255)     NOT NULL  COMMENT 'BCrypt hash',
  `role`          ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
  `created_at`    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_username` (`username`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Dashboard users — password stored as BCrypt hash';

-- ---------------------------------------------------------------------------
-- 6. View: v_violation_ts
--    Come violation ma con violationTimestamp convertito in DATETIME leggibile.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW `v_violation_ts` AS
SELECT
  `id`,
  `violationMessage`,
  `probeNameThatTriggersError`,
  `ruleViolatedName`,
  `violationTimestamp`,
  FROM_UNIXTIME(`violationTimestamp` / 1000)  AS `violation_time`,
  `ruleMetadata`
FROM `violation`;

-- ---------------------------------------------------------------------------
-- 7. View: v_event_ts  (bonus — timestamp leggibile anche per gli eventi)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW `v_event_ts` AS
SELECT
  `id`,
  `senderID`,
  `timestamp`,
  FROM_UNIXTIME(`timestamp` / 1000)  AS `event_time`,
  `dataClassName`
FROM `event`;

-- =============================================================================
-- Fine script
-- =============================================================================

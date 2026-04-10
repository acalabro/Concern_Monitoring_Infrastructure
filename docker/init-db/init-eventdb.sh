#!/bin/bash
# init-eventdb.sh
# Questo script viene eseguito automaticamente da MySQL all'avvio del container

set -e

echo "Initializing eventdb schema..."

mysql -u concern -pun53cur3!! eventdb << 'EOSQL'

-- Crea tabella event
CREATE TABLE IF NOT EXISTS `event` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `senderID` varchar(255) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  `data` longblob NOT NULL,
  `dataClassName` varchar(512) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_senderID` (`senderID`),
  KEY `idx_timestamp` (`timestamp`),
  KEY `idx_event_sender_timestamp` (`senderID`,`timestamp`),
  KEY `idx_event_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crea tabella violation
CREATE TABLE IF NOT EXISTS `violation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `violationMessage` text DEFAULT NULL,
  `probeNameThatTriggersError` varchar(255) DEFAULT NULL,
  `ruleViolatedName` varchar(255) DEFAULT NULL,
  `violationTimestamp` bigint(20) NOT NULL,
  `ruleMetadata` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_violation_probe_timestamp` (`probeNameThatTriggersError`,`violationTimestamp`),
  KEY `idx_violation_rule_timestamp` (`ruleViolatedName`,`violationTimestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crea view v_violation_ts
CREATE OR REPLACE VIEW `v_violation_ts` AS 
SELECT 
  `id`,
  `violationMessage`,
  `probeNameThatTriggersError`,
  `ruleViolatedName`,
  `violationTimestamp`,
  FROM_UNIXTIME(`violationTimestamp`/1000) AS `violation_time`,
  `ruleMetadata`
FROM `violation`;

EOSQL

echo "Database schema created successfully!"

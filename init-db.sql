CREATE DATABASE IF NOT EXISTS eventdb;
USE eventdb;

CREATE TABLE IF NOT EXISTS violation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    violationMessage TEXT,
    probeNameThatTriggersError VARCHAR(255),
    ruleViolatedName VARCHAR(255),
    violationTimestamp BIGINT,
    ruleMetadata JSON,
    INDEX idx_violation_timestamp (violationTimestamp),
    INDEX idx_rule_name (ruleViolatedName),
    INDEX idx_probe_name (probeNameThatTriggersError)
);

CREATE TABLE IF NOT EXISTS event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    senderID VARCHAR(255),
    timestamp BIGINT,
    data JSON,
    dataClassName VARCHAR(255),
    INDEX idx_event_timestamp (timestamp),
    INDEX idx_sender_id (senderID),
    INDEX idx_data_class (dataClassName)
);

-- Crea utente per Grafana
CREATE USER IF NOT EXISTS 'grafana'@'%' IDENTIFIED BY 'grafana123';
GRANT SELECT ON eventdb.* TO 'grafana'@'%';
FLUSH PRIVILEGES;

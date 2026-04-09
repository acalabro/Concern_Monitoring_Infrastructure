CREATE DATABASE IF NOT EXISTS eventdb;
USE eventdb;

CREATE TABLE violation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    violationMessage TEXT,
    probeNameThatTriggersError VARCHAR(255),
    ruleViolatedName VARCHAR(255),
    violationTimestamp BIGINT,
    ruleMetadata JSON
);

CREATE TABLE event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    senderID VARCHAR(255),
    timestamp BIGINT,
    data JSON,
    dataClassName VARCHAR(255)
);

CREATE USER IF NOT EXISTS 'concern'@'%' IDENTIFIED BY 'un53cur3!!';
GRANT ALL PRIVILEGES ON eventdb.* TO 'concern'@'%';
FLUSH PRIVILEGES;
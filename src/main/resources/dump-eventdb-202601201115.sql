/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.8.5-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: eventdb
-- ------------------------------------------------------
-- Server version	11.8.5-MariaDB-3 from Debian

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `event`
--

DROP TABLE IF EXISTS `event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `event` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `senderID` varchar(255) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  `data` longblob NOT NULL,
  `dataClassName` varchar(512) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_senderID` (`senderID`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=446349 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event`
--

LOCK TABLES `event` WRITE;
/*!40000 ALTER TABLE `event` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `event` VALUES
(1,'ENCRYPTED_Probe',1768833754958,'m0qjxD/Udj8i9VMG96Z36Q==','java.lang.String'),
(2,'ENCRYPTED_Probe',1768833754992,'oyJWwVwWqz448nAkeq4LVQ==','java.lang.String'),
(3,'ENCRYPTED_Probe',1768833755027,'iNvJKojmlLRLZ/QNEZnYJw==','java.lang.String'),
(4,'ENCRYPTED_Probe',1768833755059,'ww6kCq/IMcxPTJE2yDEQZA==','java.lang.String'),
(5,'ENCRYPTED_Probe',1768833755072,'kcPshaunkTACI3UKbLNo+w==','java.lang.String'),
(6,'ENCRYPTED_Probe',1768833755089,'xp3+bq8SYP6Lv0y2hsMf9g==','java.lang.String'),
(7,'ENCRYPTED_Probe',1768833755116,'zIZkc8cI+3poQT3IfbHkpg==','java.lang.String'),
(8,'ENCRYPTED_Probe',1768833755147,'nC4fpJYF8TgXBKM6WOQcag==','java.lang.String'),
(9,'ENCRYPTED_Probe',1768833755165,'ZkX3GIwfRg3n0qCP9USe2w==','java.lang.String'),
(10,'ENCRYPTED_Probe',1768833755192,'XTuLuSTIeYhe77xcT2/POQ==','java.lang.String'),
(11,'ENCRYPTED_Probe',1768833755231,'t1xormvpJwF35CAf8iY2Kg==','java.lang.String'),
(12,'ENCRYPTED_Probe',1768833755250,'fdFKqfFCLM4vInSnHmpPUw==','java.lang.String'),
(13,'ENCRYPTED_Probe',1768833755281,'y2FN0aP0G9g9tjiN1O6yFw==','java.lang.String'),
(14,'ENCRYPTED_Probe',1768833755292,'npUZpyti4VjiFjrLuSau9A==','java.lang.String'),
(15,'ENCRYPTED_Probe',1768833755317,'QWtoCtly6p9noZwRYyvrlw==','java.lang.String'),
(16,'ENCRYPTED_Probe',1768833755356,'VbJLpejC5+dUjw/38/X4/g==','java.lang.String'),
(17,'ENCRYPTED_Probe',1768833755386,'du8gtrbsCEKdWiaRbor7xA==','java.lang.String'),
(18,'ENCRYPTED_Probe',1768833755398,'48GnpyH+2yhubNf1q3a2/g==','java.lang.String'),
(19,'ENCRYPTED_Probe',1768833755437,'1h5QpiKI0zIlhoMIdt/3+w==','java.lang.String'),
(20,'ENCRYPTED_Probe',1768833755457,'Z62AHvBY3Z8xxFLZZZ6j0g==','java.lang.String'),
(21,'ENCRYPTED_Probe',1768833755462,'aecZ9BkHWXJGFMWJ1m7GHw==','java.lang.String'),
(22,'ENCRYPTED_Probe',1768833755485,'APvu6r4AjgDlXV288clsQQ==','java.lang.String'),
(23,'ENCRYPTED_Probe',1768833755506,'mAkrch6Ww5DoVKCX0F7nKQ==','java.lang.String'),
(24,'ENCRYPTED_Probe',1768833755542,'jBuS/fToZH8cDaNFIFvQhA==','java.lang.String'),
(25,'ENCRYPTED_Probe',1768833755571,'64Wg66lrjz4/gsNk+W45WA==','java.lang.String'),
(26,'ENCRYPTED_Probe',1768833755593,'CEnnQKC9ZFDLQsNxD0Ko5Q==','java.lang.String'),
(27,'ENCRYPTED_Probe',1768833755603,'12vEjlHgZL12cnW+loauCA==','java.lang.String'),
(28,'ENCRYPTED_Probe',1768833755632,'XnmQWdE8vxliTJsI/6I6Tg==','java.lang.String'),
(29,'ENCRYPTED_Probe',1768833755661,'W8Uu3tQ0RJ4Yh8Yzyy+WwQ==','java.lang.String'),
(30,'ENCRYPTED_Probe',1768833755670,'JnwxVbwLxZheh2d8CzJP7A==','java.lang.String'),
(31,'ENCRYPTED_Probe',1768833755676,'6kEpjKdo6JCrvh5SIJuoSA==','java.lang.String'),
(32,'ENCRYPTED_Probe',1768833755699,'Ch5dvbsbf3g+9rXBCx5S1w==','java.lang.String'),
(33,'ENCRYPTED_Probe',1768833755715,'y6kXAkDLr9BbU3WiOUh3Mw==','java.lang.String'),
(34,'ENCRYPTED_Probe',1768833755731,'ND+vr3zcD05LWgBeWJ4mFQ==','java.lang.String'),
(35,'ENCRYPTED_Probe',1768833755764,'NT7+zKNunP8C3gHKawCx9g==','java.lang.String'),
(36,'ENCRYPTED_Probe',1768833755772,'k5a8NHHaC+EbsojO9rPq5A==','java.lang.String'),
(37,'ENCRYPTED_Probe',1768833755788,'D2kr+vOTmZo6KX40OkqfvQ==','java.lang.String'),
(38,'ENCRYPTED_Probe',1768833755808,'lFz7sipav6XJtqHqatGAOw==','java.lang.String'),
(39,'ENCRYPTED_Probe',1768833755841,'WUuTMqgVVBJZKVB9BIWxwg==','java.lang.String'),
(40,'ENCRYPTED_Probe',1768833755871,'rYYzvyCUR9GEfW/S7W5vYg==','java.lang.String'),
(41,'ENCRYPTED_Probe',1768833755880,'BbmGrStMKcLpjr1GG45S6w==','java.lang.String'),
(42,'ENCRYPTED_Probe',1768833755901,'fTnV6Pl2fDQe8FqyJuNWDA==','java.lang.String'),
(43,'ENCRYPTED_Probe',1768833755925,'GoB4EphAHX5sD9MeyHCiEg==','java.lang.String'),
(44,'ENCRYPTED_Probe',1768833755965,'LkYFQmXczYl2AzyuQYFPCA==','java.lang.String'),
(45,'ENCRYPTED_Probe',1768833755973,'D4/+8NbRoqNeyPXyeWtC8Q==','java.lang.String'),
(46,'ENCRYPTED_Probe',1768833756009,'KdirDGLwKdXNE0IZZR8F1w==','java.lang.String'),
(47,'ENCRYPTED_Probe',1768833756034,'8SSh61UypeJJX4J6irJzUA==','java.lang.String'),
(48,'ENCRYPTED_Probe',1768833756060,'9BCRRFAvvWgu548BMf7CsA==','java.lang.String'),
(49,'ENCRYPTED_Probe',1768833756084,'FxLvgs6SZEU5hTMRHs0WZQ==','java.lang.String'),
(50,'ENCRYPTED_Probe',1768833756113,'3s/hJ3je/MWRRB73VXBAyQ==','java.lang.String'),
(51,'ENCRYPTED_Probe',1768833756147,'vJB5vEDXFJD1yRVL8zLZOw==','java.lang.String'),
(52,'ENCRYPTED_Probe',1768833756172,'oKNCd3+SPaxLN2dWL7pY5w==','java.lang.String'),
(53,'ENCRYPTED_Probe',1768833756182,'0QbrFdVjoP8QBDix3dGs0Q==','java.lang.String'),
(54,'ENCRYPTED_Probe',1768833756186,'0Bq93jafTfQ9u75S3Y3LYQ==','java.lang.String'),
(55,'ENCRYPTED_Probe',1768833756194,'R2ey8BNvbOZqBnmop+dvfA==','java.lang.String'),
(56,'ENCRYPTED_Probe',1768833756201,'P2+eurwBc6NCnfP0+X35Zg==','java.lang.String'),
(57,'ENCRYPTED_Probe',1768833756236,'bqsCRBICC2+aEvRaP/fMOg==','java.lang.String'),
(58,'ENCRYPTED_Probe',1768833756250,'BQMqQky+liNm/sgvUgwEIw==','java.lang.String'),
(59,'ENCRYPTED_Probe',1768833756278,'hBIUg6RSudmPvTANNA9PUg==','java.lang.String'),
(60,'ENCRYPTED_Probe',1768833756299,'ry97RcQsHS0MDZpYYR+yPA==','java.lang.String'),
(61,'ENCRYPTED_Probe',1768833756333,'xL+4zzg9sjHshZ6uAlNUnw==','java.lang.String'),
(62,'ENCRYPTED_Probe',1768833756336,'dnpggFJLgcWGVFYi/7eq+Q==','java.lang.String'),
(63,'ENCRYPTED_Probe',1768833756354,'PZ/HCKQvKZgnDTUtcrnhgQ==','java.lang.String'),
(64,'ENCRYPTED_Probe',1768833756388,'hXyCsBVD/m86H4hcmXw6uw==','java.lang.String'),
(65,'ENCRYPTED_Probe',1768833756399,'IOHQmkXHgz1BNFqyyas/Vg==','java.lang.String'),
(66,'ENCRYPTED_Probe',1768833756434,'W6q+T3JsAclgXWoRXEDbLw==','java.lang.String'),
(67,'ENCRYPTED_Probe',1768833756449,'i35M4n1nP2AqYPALPLBadA==','java.lang.String'),
(68,'ENCRYPTED_Probe',1768833756455,'TdoE+VQKt125mQgimctGjQ==','java.lang.String'),
(69,'ENCRYPTED_Probe',1768833756489,'oxAdd1dg4kJDq7cgO0qX7g==','java.lang.String'),
(70,'ENCRYPTED_Probe',1768833756514,'4NvXiYZ347pwJhk2fn22LQ==','java.lang.String'),
(71,'ENCRYPTED_Probe',1768833756528,'5LgthxvnW1BvnXQ0MUhJWw==','java.lang.String'),
(72,'ENCRYPTED_Probe',1768833756553,'ArXKhBOj0eSPUtVQyoc4zg==','java.lang.String'),
(73,'ENCRYPTED_Probe',1768833756558,'H8ZSPx18nDb7U2t7NiccKw==','java.lang.String'),
(74,'ENCRYPTED_Probe',1768833756581,'Omt73+El19vFDnn/Enj8ZA==','java.lang.String'),
(75,'ENCRYPTED_Probe',1768833756609,'B+AN6GpnV7SZuvrRGGHmEg==','java.lang.String'),
(76,'ENCRYPTED_Probe',1768833756648,'jaTB+aNpztrrLMuH00g9vg==','java.lang.String'),
(77,'ENCRYPTED_Probe',1768833756674,'FftDFAWGLaRogLIZBh3bgA==','java.lang.String'),
(78,'ENCRYPTED_Probe',1768833756677,'j5cIKBYe4GwuoUU2M5jxNQ==','java.lang.String'),
(79,'ENCRYPTED_Probe',1768833756708,'WngcduJQO4ykNwozVodw+g==','java.lang.String'),
(80,'ENCRYPTED_Probe',1768833756735,'S89zpLlpUeEVhSG90vwWmA==','java.lang.String'),
(81,'ENCRYPTED_Probe',1768833756763,'f6lO5kiNVeBf5Fe6OaSdAA==','java.lang.String'),
(82,'ENCRYPTED_Probe',1768833756802,'KGdhaaTSu9bGP5aBov+wkg==','java.lang.String'),
(83,'ENCRYPTED_Probe',1768833756810,'IuOhbV1TMk6N03sj2fOVfA==','java.lang.String'),
(84,'ENCRYPTED_Probe',1768833756844,'HP78NMOUf+vAne5jlcyTRw==','java.lang.String'),
(85,'ENCRYPTED_Probe',1768833756880,'gV9hODalmXXCoUw+jLjuOw==','java.lang.String'),
(86,'ENCRYPTED_Probe',1768833756911,'kSSURlIiKBdCTxdbAFo9ew==','java.lang.String'),
(87,'ENCRYPTED_Probe',1768833756944,'y6VJV48Hbm6CbzBbNFPbMQ==','java.lang.String'),
(88,'ENCRYPTED_Probe',1768833756983,'7UBXLcdZN4bW8t1PI/ojdQ==','java.lang.String'),
(89,'ENCRYPTED_Probe',1768833757004,'ccpeQ6SaEk+iJ1hfY04SzQ==','java.lang.String'),
(90,'ENCRYPTED_Probe',1768833757026,'QmpInGvhWDSsINi4fqHU4A==','java.lang.String'),
(91,'ENCRYPTED_Probe',1768833757050,'vZRw9DMy9u+bU4RWXDRqWA==','java.lang.String'),
(92,'ENCRYPTED_Probe',1768833757078,'btdsNsNudid6sazG0pdlsw==','java.lang.String'),
(93,'ENCRYPTED_Probe',1768833757085,'xJ947CPlw8MJal0dCMirsA==','java.lang.String'),
(94,'ENCRYPTED_Probe',1768833757089,'5VJFjJqd5UNbeWOOq+OQmw==','java.lang.String'),
(95,'ENCRYPTED_Probe',1768833757106,'Hp7OLOvQ+YyGszI62sKMAA==','java.lang.String'),
(96,'ENCRYPTED_Probe',1768833757139,'MCVEGpYyp8OfFYENeFWXWg==','java.lang.String'),
(97,'ENCRYPTED_Probe',1768833757145,'ViSQvVrwfEOTLikGIlx8zg==','java.lang.String'),
(98,'ENCRYPTED_Probe',1768833757174,'ypz+4bjMe1vU8uebJbY1rg==','java.lang.String'),
(99,'ENCRYPTED_Probe',1768833757186,'48wVQZWoQU+0vy+OIIqAYg==','java.lang.String'),
(100,'ENCRYPTED_Probe',1768833757194,'/qC49ZVsh8g0fTy8e8EUuw==','java.lang.String');
/*!40000 ALTER TABLE `event` ENABLE KEYS */;
UNLOCK TABLES;
commit;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-01-20 11:15:18

-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               11.8.4-MariaDB - MariaDB Server
-- Server OS:                    Win64
-- HeidiSQL Version:             12.8.0.6908
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for shoppingdb
CREATE DATABASE IF NOT EXISTS `shoppingdb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci */;
USE `shoppingdb`;

-- Dumping structure for table shoppingdb.categories
CREATE TABLE IF NOT EXISTS `categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `country` varchar(100) DEFAULT NULL,
  `imgURL` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt8o6pivur7nn124jehx7cygw5` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.categories: ~12 rows (approximately)
REPLACE INTO `categories` (`id`, `name`, `country`, `imgURL`) VALUES
	(1, 'Chanel', 'Pháp', '	https://static.vecteezy.com/system/resources/thumb…black-design-fashion-illustration-free-vector.jpg'),
	(2, 'Dior', 'Pháp', '	https://i.pinimg.com/1200x/73/9c/18/739c182ecf9a3e7a749690b337cde4b2.jpg'),
	(3, 'Gucci', 'Ý', '	https://thumbs.dreamstime.com/b/gucci-logo-editori…-icons-set-social-media-flat-banner-208332316.jpg'),
	(4, 'Tom Ford', 'Mỹ', 'https://www.perfumeplusdirect.co.uk/media/codazon_…d/400x400/wysiwyg/codazon/brand/Tom-Ford-Logo.png'),
	(5, 'Creed', 'Pháp', 'https://i.pinimg.com/736x/64/5c/90/645c9068c4113d7fdc46ad615eeffc3b.jpg'),
	(7, 'Versace', 'Ý', 'https://images.seeklogo.com/logo-png/14/1/versace-medusa-logo-png_seeklogo-148376.png'),
	(8, 'Yves Saint Laurent', 'Pháp', '	https://images.seeklogo.com/logo-png/28/1/yves-saint-laurent-logo-png_seeklogo-288938.png'),
	(9, 'Giorgio Armani', 'Ý', 'https://images.seeklogo.com/logo-png/39/1/giorgio-armani-logo-png_seeklogo-393860.png'),
	(10, 'Jo Malone', 'Anh', '	https://vipyidiancom.oss-cn-beijing.aliyuncs.com/vipyidian.com/article/0232022090848454.jpg'),
	(11, 'Le Labo', 'Mỹ', 'https://images.seeklogo.com/logo-png/31/1/le-labo-logo-png_seeklogo-316974.png'),
	(12, 'Morra', 'Việt Nam', 'https://interbra.vn/public/DATA/2022/4-2022-46928.jpg'),
	(13, 'ViinRiic', 'Việt Nam', '	https://viinriic.com/wp-content/uploads/2019/06/logo-Mendittorosa-VR-300x300.jpg'),
	(14, 'Test', 'Thái lan', 'http://res.cloudinary.com/dlvtnwywp/image/upload/v1763732282/shopnuochoa_products/vtdiljikcfub4pgcdoci.png');

-- Dumping structure for table shoppingdb.comments
CREATE TABLE IF NOT EXISTS `comments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `text` text DEFAULT NULL,
  `product_id` int(11) NOT NULL,
  `rating` int(11) NOT NULL,
  `customer_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6uv0qku8gsu6x1r2jkrtqwjtn` (`product_id`),
  KEY `fk_comment_customer` (`customer_id`),
  CONSTRAINT `FK6uv0qku8gsu6x1r2jkrtqwjtn` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_comment_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.comments: ~2 rows (approximately)
REPLACE INTO `comments` (`id`, `text`, `product_id`, `rating`, `customer_id`, `created_at`) VALUES
	(17, 'thơm nha', 2, 5, 8, '2025-11-20 02:10:54');

-- Dumping structure for table shoppingdb.customers
CREATE TABLE IF NOT EXISTS `customers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `customer_since` date NOT NULL,
  `name` varchar(255) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `gender` enum('NAM','NU','UNISEX') DEFAULT NULL,
  `id_card` varchar(255) DEFAULT NULL,
  `nick_name` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `fk_customer_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.customers: ~8 rows (approximately)
REPLACE INTO `customers` (`id`, `customer_since`, `name`, `user_id`, `address`, `birthday`, `email`, `gender`, `id_card`, `nick_name`, `phone_number`) VALUES
	(1, '2024-01-20', 'Nguyễn Văn An', 1, '', '0000-00-00', '', NULL, '', '', ''),
	(3, '2024-05-22', 'Lê Hoàng Cường', 3, '', '0000-00-00', '', NULL, '', '', ''),
	(4, '2024-07-30', 'Phạm Thị Diệu', 4, '', '0000-00-00', '', NULL, '', '', ''),
	(5, '2024-09-05', 'Võ Minh Em', 5, '', '0000-00-00', '', NULL, '', '', ''),
	(6, '2025-10-12', 'Nguyễn Danh Minh Toàn', 6, '', '0000-00-00', '', NULL, '', '', ''),
	(7, '2025-10-15', 'Chu cha mà ơi', 10, '', '0000-00-00', '', NULL, '', '', ''),
	(8, '2025-10-19', 'Trần Phương Nhi', 11, '2345/13 Pham The Hien', '2025-11-20', '', 'NU', '', '', '0765593697'),
	(9, '2025-11-15', 'admin', 12, '', '0000-00-00', '', NULL, '', '', ''),
	(10, '2025-11-21', 'Nguyễn Minh Toàn', 14, NULL, NULL, 'mtoan0547@gmail.com', NULL, NULL, NULL, NULL);

-- Dumping structure for table shoppingdb.orders
CREATE TABLE IF NOT EXISTS `orders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` datetime(6) NOT NULL,
  `customer_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpxtb8awmi0dk6smoh2vp1litg` (`customer_id`),
  CONSTRAINT `FKpxtb8awmi0dk6smoh2vp1litg` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.orders: ~23 rows (approximately)
REPLACE INTO `orders` (`id`, `date`, `customer_id`) VALUES
	(1, '2025-08-10 10:30:00.000000', 1),
	(3, '2025-09-01 09:15:00.000000', 1),
	(4, '2025-09-20 18:00:00.000000', 3),
	(5, '2025-10-05 11:45:00.000000', 4),
	(12, '2025-10-13 14:34:20.940197', 1),
	(13, '2025-10-14 14:25:22.519747', 1),
	(14, '2025-10-15 15:40:26.321962', 7),
	(15, '2025-10-17 15:02:11.315567', 7),
	(16, '2025-10-17 15:05:36.181008', 7),
	(17, '2025-10-17 15:06:19.843543', 7),
	(18, '2025-10-17 15:16:38.223658', 7),
	(19, '2025-10-18 12:01:37.584176', 7),
	(20, '2025-10-19 21:34:37.844631', 8),
	(21, '2025-11-07 02:17:45.831798', 8),
	(22, '2025-11-08 14:29:44.219297', 8),
	(23, '2025-11-11 00:16:13.232524', 8),
	(24, '2025-11-16 00:44:13.090901', 8),
	(25, '2025-11-16 00:52:28.993997', 8),
	(26, '2025-11-16 13:57:03.593952', 8),
	(27, '2025-11-17 01:39:50.291874', 8),
	(28, '2025-11-17 01:59:21.498729', 8),
	(29, '2025-11-17 02:10:00.103657', 8),
	(30, '2025-11-17 02:13:40.154595', 8),
	(31, '2025-11-19 21:14:01.703288', 8),
	(32, '2025-11-20 00:32:23.485406', 8),
	(33, '2025-11-20 19:44:42.429271', 8),
	(34, '2025-11-20 20:15:21.436892', 8);

-- Dumping structure for table shoppingdb.order_lines
CREATE TABLE IF NOT EXISTS `order_lines` (
  `amount` int(11) NOT NULL,
  `purchase_price` decimal(38,2) NOT NULL,
  `order_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  PRIMARY KEY (`order_id`,`product_id`),
  KEY `FK5v1oeejtgtf2n3toppm3tkuhh` (`product_id`),
  CONSTRAINT `FK1smc0s578t2oih21yn9hw6usr` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FK5v1oeejtgtf2n3toppm3tkuhh` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.order_lines: ~26 rows (approximately)
REPLACE INTO `order_lines` (`amount`, `purchase_price`, `order_id`, `product_id`) VALUES
	(1, 32500000.00, 1, 1),
	(1, 150000.00, 3, 3),
	(3, 55000.00, 3, 5),
	(1, 550000.00, 4, 2),
	(10, 55000.00, 5, 5),
	(1, 32500000.00, 12, 1),
	(1, 550000.00, 13, 2),
	(1, 150000.00, 13, 3),
	(1, 32500000.00, 14, 1),
	(1, 32500000.00, 15, 1),
	(1, 32500000.00, 16, 1),
	(1, 150000.00, 17, 3),
	(1, 32500000.00, 18, 1),
	(2, 550000.00, 18, 2),
	(1, 32500000.00, 19, 1),
	(1, 32500000.00, 20, 1),
	(1, 100000.00, 21, 14),
	(3, 100000.00, 22, 14),
	(1, 32500000.00, 23, 1),
	(1, 100000.00, 24, 14),
	(2, 100000.00, 25, 14),
	(1, 100000.00, 26, 14),
	(1, 100000.00, 27, 14),
	(1, 100000.00, 28, 14),
	(1, 100000.00, 29, 14),
	(1, 100000.00, 30, 14),
	(1, 2500000.00, 31, 14),
	(1, 2500000.00, 32, 14),
	(1, 2800000.00, 33, 2),
	(1, 2500000.00, 34, 14);

-- Dumping structure for table shoppingdb.products
CREATE TABLE IF NOT EXISTS `products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `price` double NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0,
  `category_id` int(11) NOT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `hot_trend` tinyint(1) DEFAULT 0,
  `volume` varchar(50) DEFAULT NULL,
  `gender` varchar(20) DEFAULT NULL,
  `average_rating` double DEFAULT 0,
  `rating_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`),
  CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.products: ~13 rows (approximately)
REPLACE INTO `products` (`id`, `name`, `price`, `quantity`, `category_id`, `image_url`, `hot_trend`, `volume`, `gender`, `average_rating`, `rating_count`) VALUES
	(1, 'Chanel N5 Eau de Parfum', 3500000, 50, 1, 'https://minhtuanmobile.com/uploads/products/231209053742-iphone-15-256gb.jpg', 1, 'ML_100', 'NU', 0, NULL),
	(2, 'Bleu de Chanel Eau de Toilette', 2800000, 69, 1, 'https://www.ecosmetics.com/wp-content/uploads/2022/10/3145891073508-600x600.jpg', 0, 'ML_100', 'NAM', 5, 1),
	(3, 'Sauvage Eau de Parfum', 3100000, 80, 2, 'https://mfparis.vn/wp-content/uploads/2021/08/nuoc…e-eau-de-parfum-Ebc0juc6QmuP5n47-mfparis.jpg.webp', 1, 'ML_100', 'NAM', 0, NULL),
	(4, 'J\'adore Eau de Parfum', 3800000, 45, 2, 'https://cdn.vuahanghieu.com/unsafe/0x900/left/top/…-de-parfum-100ml-673448cc3e6c2-13112024133556.jpg', 0, 'ML_100', 'NU', 0, NULL),
	(5, 'Gucci Bloom Eau de Parfum', 2950000, 60, 3, 'https://storage.beautyfulls.com/uploads-1/avatar/product/1200x/2022/10/05/figure-1664937209959.png', 0, 'ML_100', 'NU', 0, NULL),
	(7, 'Creed Aventus', 8200000, 25, 5, 'https://product.hstatic.net/1000340570/product/cre…us-100ml-edp_d6a9b983c5d94cf2a48504f3dc8ae461.jpg', 1, 'ML_100', 'NAM', 0, NULL),
	(8, 'Versace Eros Eau de Toilette', 2100000, 100, 1, 'https://cdn.vuahanghieu.com/unsafe/0x900/left/top/…os-man-edt-200ml-67510c71d6608-05122024091409.jpg', 0, 'ML_100', 'NAM', 0, NULL),
	(9, 'YSL Black Opium', 3300000, 55, 1, 'https://cdn.vuahanghieu.com/unsafe/0x900/left/top/…m-women-edp-50ml-64054a5c57554-06032023090516.jpg', 0, 'ML_75', 'NU', 0, NULL),
	(10, 'Acqua di Giò Profondo', 2900000, 65, 1, 'https://product.hstatic.net/1000340570/product/gio…100ml_f7faab6384b84bd3b4a4e2f0bd68497a_master.jpg', 0, 'ML_200', 'NAM', 0, NULL),
	(11, 'Jo Malone English Pear & Freesia', 3600000, 40, 1, 'https://cdn.vuahanghieu.com/unsafe/0x900/left/top/…ia-cologne-100ml-62061032d2047-11022022142850.jpg', 0, 'ML_100', 'UNISEX', 0, NULL),
	(12, 'Le Labo Santal 33', 7500000, 20, 1, 'https://orchard.vn/wp-content/uploads/2024/07/le-labo-santal-33_2.jpg', 0, 'ML_100', 'UNISEX', 0, NULL),
	(13, 'Morra - Gỗ Đàn Hương (Sandalwood)', 850000, 50, 1, 'https://salt.tikicdn.com/ts/tmp/70/92/69/c72a1e98a98f936db20decc31877175d.png', 0, 'ML_50', 'UNISEX', 0, NULL),
	(14, 'ViinRiic - Mưa Sài Gòn (Saigon Rain)', 2500000, 32, 12, 'https://viinriic.com/wp-content/uploads/2021/04/1.…che-perfume_beloved-man-amouage_elle-man_0920.jpg', 0, 'ML_50', 'UNISEX', 0, 0),
	(17, 'Chanel', 10000000, 100, 1, 'http://res.cloudinary.com/dlvtnwywp/image/upload/v1763730686/shopnuochoa_products/zsha46v8lcnxdeogrk1h.png', 0, 'ML_150', 'NAM', 0, NULL);

-- Dumping structure for table shoppingdb.users
CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','CUSTOMER','GUEST') NOT NULL DEFAULT 'CUSTOMER',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `full_name` varchar(100) NOT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.users: ~11 rows (approximately)
REPLACE INTO `users` (`id`, `username`, `email`, `password_hash`, `role`, `is_active`, `created_at`, `full_name`, `avatar`) VALUES
	(1, 'an_nguyen', 'an.nguyen@example.com', '$2a$10$BhmsPnot4Q6B.58XS6XuYeeIMqsYgnfs8YzBaS7dum67RV/ZbV/Dy', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Nguyễn Văn An', NULL),
	(3, 'cuong_le', 'cuong.le@example.com', '$2a$10$aB.cDeFgHijklMnoPqRsTuVwXyZaBcDeFgHijklMnoPq', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Lê Hoàng Cường', NULL),
	(4, 'dieu_pham', 'dieu.pham@example.com', '$2a$10$9rStUvWxYzAbCdEfGhIjKu8lMnOpQrStUvWxYzAbCdEfGh', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Phạm Thị Diệu', NULL),
	(5, 'em_vo', 'em.vo@example.com', '$2a$10$kL.mNoPqRsTuVwXyZaBcDeFgHijklMnoPqRsTuVwXyZ', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Võ Minh Em', NULL),
	(6, 'toan_nguyen', 'toan.nguyen@example.com', '$2a$10$oPqRsTuVwXyZaBcDeFgHijklMnoPqRsTuVwXyZaBcDeF', 'ADMIN', 1, '2025-10-14 06:46:54', 'Nguyễn Danh Minh Toàn', NULL),
	(8, 'test', 'toan@gmail.com', '$2a$10$yUQ2Lr8/R7zHvAwXrqYwG.Xa0vagtPdGfVAqwsq3.5vGOvhWHwxbe', 'CUSTOMER', 1, '2025-10-15 05:57:42', 'Nguyen danh minh', NULL),
	(9, 'noat04', 'toannguyen041214@gmail.com', '$2a$10$j4eVa.jsqwngqkzIzH674uYc5f65rmEYoerw5mS6ZKVATXrWkHbvS', 'CUSTOMER', 1, '2025-10-15 06:29:43', 'Nguyễn Danh Minh Toàn', NULL),
	(10, 'chu', 'chu@gmail.com', '$2a$10$y225smkQ834mzGJPgIiV4OfoNhWTohN9w96upZ0FY.mjAww//V/tG', 'ADMIN', 1, '2025-10-15 08:13:16', 'Chu cha mà ơi', NULL),
	(11, 'nhi', 'nhiPhuong@gmail.com', '$2a$10$bzGBNkIT5CLGiL6iGpkVEeDJIrY4t7aBSkKHGWJyRGz6bOAMzRMhC', 'CUSTOMER', 1, '2025-10-19 05:42:48', 'Trần Phương Nhi', 'http://res.cloudinary.com/dlvtnwywp/image/upload/v1763709206/shopnuochoa_avatars/mwzse0gmftstiflaflhn.png'),
	(12, 'admin', 'admin@gmail.com', '$2a$10$jtS6OIDroUy/3vJgzMZJlOFIGt3XhrbN7hYG9r3diPwDezULNbQke', 'ADMIN', 1, '2025-11-14 19:46:52', 'admin', NULL),
	(14, 'toan', 'mtoan0547@gmail.com', '$2a$10$LZjaKSuJWOGI34ygFRXdpOI3ZdpBjoJ3yaSomiQ/ecGLdbKzVGJQC', 'CUSTOMER', 1, '2025-11-21 16:55:16', 'Nguyễn Minh Toàn', NULL);

-- Dumping structure for table shoppingdb.vector_store
CREATE TABLE IF NOT EXISTS `vector_store` (
  `id` uuid NOT NULL,
  `content` tinytext NOT NULL,
  `metadata` tinytext DEFAULT NULL,
  `embedding` blob DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table shoppingdb.vector_store: ~0 rows (approximately)

-- Dumping structure for table shoppingdb.wishlist
CREATE TABLE IF NOT EXISTS `wishlist` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `product_id` int(11) NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `customer_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6p7qhvy1bfkri13u29x6pu8au` (`product_id`),
  KEY `FKk6lal9w7ut5e4xvta479rq06y` (`customer_id`),
  CONSTRAINT `FK6p7qhvy1bfkri13u29x6pu8au` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKk6lal9w7ut5e4xvta479rq06y` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- Dumping data for table shoppingdb.wishlist: ~0 rows (approximately)

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;

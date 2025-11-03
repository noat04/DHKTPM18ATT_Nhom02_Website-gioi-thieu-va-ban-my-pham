-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               11.6.2-MariaDB - mariadb.org binary distribution
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

-- Dumping data for table shoppingdb.categories: ~5 rows (approximately)
REPLACE INTO `categories` (`id`, `name`) VALUES
	(1, 'Điện tử mới'),
	(4, 'Đồ gia dụng'),
	(3, 'Sách'),
	(2, 'Thời trang'),
	(5, 'Thực phẩm');

-- Dumping data for table shoppingdb.comments: ~7 rows (approximately)
REPLACE INTO `comments` (`id`, `text`, `product_id`) VALUES
	(1, 'Điện thoại tuyệt vời, camera siêu nét!', 1),
	(2, 'Váy mặc lên rất xinh, vải mát.', 2),
	(3, 'Giao hàng nhanh, sách được bọc cẩn thận.', 3),
	(4, 'Chất lượng cà phê thơm ngon, sẽ ủng hộ tiếp.', 5),
	(5, 'Sản phẩm tốt, đáng tiền.', 1),
	(6, 'Anh làm app này đẹp trai quá', 1),
	(7, 'Đỉnh quá anh ơi', 1);

-- Dumping data for table shoppingdb.customers: ~7 rows (approximately)
REPLACE INTO `customers` (`id`, `customer_since`, `name`, `user_id`) VALUES
	(1, '2024-01-20', 'Nguyễn Văn An', 1),
	(3, '2024-05-22', 'Lê Hoàng Cường', 3),
	(4, '2024-07-30', 'Phạm Thị Diệu', 4),
	(5, '2024-09-05', 'Võ Minh Em', 5),
	(6, '2025-10-12', 'Nguyễn Danh Minh Toàn', 6),
	(7, '2025-10-15', 'Chu cha mà ơi', 10),
	(8, '2025-10-19', 'Trần Phương Nhi', 11);

-- Dumping data for table shoppingdb.orders: ~13 rows (approximately)
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
	(20, '2025-10-19 21:34:37.844631', 8);

-- Dumping data for table shoppingdb.order_lines: ~16 rows (approximately)
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
	(1, 32500000.00, 20, 1);

-- Dumping data for table shoppingdb.products: ~13 rows (approximately)
REPLACE INTO `products` (`id`, `in_stock`, `name`, `price`, `category_id`) VALUES
	(1, b'1', 'iPhone 15 Pro Max 256GB', 32500000, 1),
	(2, b'1', 'Váy Hoa Nhí Vintage Cổ Vuông', 550000, 2),
	(3, b'1', 'Sách "Muôn Kiếp Nhân Sinh"', 150000, 3),
	(4, b'0', 'Bộ Lau Nhà Tự Vắt Thông Minh', 250000, 4),
	(5, b'1', 'Cà Phê Hòa Tan G7 3in1', 55000, 5),
	(7, b'1', 'Laptop Acer icore5', 37000000, 1),
	(8, b'1', 'Chuột máy tính', 1000000, 1),
	(9, b'1', 'Bàn phím thu gọn', 2000000, 1),
	(10, b'1', 'Tai nghe có dây', 50000, 1),
	(11, b'1', 'Cục sạc dự phòng', 500000, 1),
	(12, b'1', 'Điện thoại iphone 17 pro', 37000000, 1),
	(13, b'1', 'Ipad Xaomi', 10000000, 1),
	(14, b'1', 'Áo Mưa', 100000, 2);

-- Dumping data for table shoppingdb.users: ~9 rows (approximately)
REPLACE INTO `users` (`id`, `username`, `email`, `password_hash`, `role`, `is_active`, `created_at`, `full_name`) VALUES
	(1, 'an_nguyen', 'an.nguyen@example.com', '$2a$10$BhmsPnot4Q6B.58XS6XuYeeIMqsYgnfs8YzBaS7dum67RV/ZbV/Dy', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Nguyễn Văn An'),
	(3, 'cuong_le', 'cuong.le@example.com', '$2a$10$aB.cDeFgHijklMnoPqRsTuVwXyZaBcDeFgHijklMnoPq', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Lê Hoàng Cường'),
	(4, 'dieu_pham', 'dieu.pham@example.com', '$2a$10$9rStUvWxYzAbCdEfGhIjKu8lMnOpQrStUvWxYzAbCdEfGh', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Phạm Thị Diệu'),
	(5, 'em_vo', 'em.vo@example.com', '$2a$10$kL.mNoPqRsTuVwXyZaBcDeFgHijklMnoPqRsTuVwXyZ', 'CUSTOMER', 1, '2025-10-14 06:46:54', 'Võ Minh Em'),
	(6, 'toan_nguyen', 'toan.nguyen@example.com', '$2a$10$oPqRsTuVwXyZaBcDeFgHijklMnoPqRsTuVwXyZaBcDeF', 'ADMIN', 1, '2025-10-14 06:46:54', 'Nguyễn Danh Minh Toàn'),
	(8, 'test', 'toan@gmail.com', '$2a$10$yUQ2Lr8/R7zHvAwXrqYwG.Xa0vagtPdGfVAqwsq3.5vGOvhWHwxbe', 'CUSTOMER', 1, '2025-10-15 05:57:42', 'Nguyen danh minh'),
	(9, 'noat04', 'toannguyen041214@gmail.com', '$2a$10$j4eVa.jsqwngqkzIzH674uYc5f65rmEYoerw5mS6ZKVATXrWkHbvS', 'CUSTOMER', 1, '2025-10-15 06:29:43', 'Nguyễn Danh Minh Toàn'),
	(10, 'chu', 'chu@gmail.com', '$2a$10$y225smkQ834mzGJPgIiV4OfoNhWTohN9w96upZ0FY.mjAww//V/tG', 'ADMIN', 1, '2025-10-15 08:13:16', 'Chu cha mà ơi'),
	(11, 'nhi', 'nhiPhuong@gmail.com', '$2a$10$bzGBNkIT5CLGiL6iGpkVEeDJIrY4t7aBSkKHGWJyRGz6bOAMzRMhC', 'CUSTOMER', 1, '2025-10-19 05:42:48', 'Trần Phương Nhi');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;

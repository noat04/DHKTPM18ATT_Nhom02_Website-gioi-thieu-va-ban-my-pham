package org.fit.shopnuochoa.Enum;

public enum OrderStatus {
    PENDING,    // Mới đặt, chờ thanh toán/xử lý
    SHIPPING,   // Đang giao hàng
    DELIVERED,  // Đã giao hàng thành công
    COMPLETED,  // Hoàn tất (Đóng đơn)
    CANCELLED   // Đã hủy
}
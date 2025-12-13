package org.fit.shopnuochoa.component;

import org.fit.shopnuochoa.Enum.OrderStatus;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

    private final OrdersRepository ordersRepository;

    public OrderScheduler(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }

    /**
     * Chạy mỗi 1 giờ (3600000ms)
     * Tự động chuyển SHIPPING -> DELIVERED dựa trên Thời gian giao hàng dự kiến
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoDeliverOrders() {
        // 1. Lấy tất cả đơn đang giao
        List<Orders> shippingOrders = ordersRepository.findByStatus(OrderStatus.SHIPPING);

        LocalDateTime now = LocalDateTime.now();

        for (Orders order : shippingOrders) {
            // 2. Lấy ngày dự kiến giao hàng từ logic trong Entity
            LocalDateTime estimatedDate = order.getEstimatedDeliveryDate();

            // 3. So sánh: Nếu thời gian hiện tại (now) đã vượt quá thời gian dự kiến
            // (Tức là shipper đã giao xong theo kế hoạch)
            if (estimatedDate != null && now.isAfter(estimatedDate)) {

                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveryDate(now); // Lưu thời gian giao thực tế là lúc này

                ordersRepository.save(order);

                System.out.println("Auto-update Order #" + order.getId() + " to DELIVERED (Est: " + estimatedDate + ")");
            }
        }
    }

    // ... (Hàm autoCompleteOrders giữ nguyên logic 3 ngày sau khi giao) ...
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoCompleteOrders() {
        List<Orders> deliveredOrders = ordersRepository.findByStatus(OrderStatus.DELIVERED);
        LocalDateTime now = LocalDateTime.now();

        for (Orders order : deliveredOrders) {
            // Nếu đã giao hàng được 3 ngày (tính từ deliveryDate thực tế) -> Hoàn tất
            if (order.getDeliveryDate() != null && now.isAfter(order.getDeliveryDate().plusDays(3))) {
                order.setStatus(OrderStatus.COMPLETED);
                ordersRepository.save(order);
                System.out.println("Auto-complete Order #" + order.getId());
            }
        }
    }
}
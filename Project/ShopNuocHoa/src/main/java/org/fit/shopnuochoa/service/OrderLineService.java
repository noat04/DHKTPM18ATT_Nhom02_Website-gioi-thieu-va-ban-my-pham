package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.OrderLine;
import org.fit.shopnuochoa.model.OrderLineId;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.OrderLineRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service // Thêm @Service để Spring quản lý
public class OrderLineService {
    private final OrderLineRepository orderLineRepository;
    private final OrderService orderService; // Dùng để lấy đối tượng Order
    private final ProductService productService; // Dùng để lấy đối tượng Product

    public OrderLineService(OrderLineRepository orderLineRepository, OrderService orderService, ProductService productService) {
        this.orderLineRepository = orderLineRepository;
        this.orderService = orderService;
        this.productService = productService;
    }

    public List<OrderLine> findAll() {
        return orderLineRepository.findAll();
    }

    public Optional<OrderLine> findById(OrderLineId id) {
        return orderLineRepository.findById(id);
    }
    /**
     * Lấy tất cả chi tiết đơn hàng cho một Order ID cụ thể.
     * Đây là hàm mà OrderController sẽ gọi để tạo Map.
     */
    public List<OrderLine> findAllByOrderId(Integer orderId) {
        return orderLineRepository.findByOrderId(orderId);
    }
    /**
     * Tạo một chi tiết đơn hàng mới.
     * @param orderLineDetails Đối tượng chứa thông tin số lượng, giá mua
     * @param orderId ID của đơn hàng
     * @param productId ID của sản phẩm
     * @return OrderLine đã được lưu
     */
    public OrderLine createOrderLine(OrderLine orderLineDetails, Integer orderId, Integer productId) {
        // 1. Lấy các đối tượng cha
        Orders order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        Product product = productService.getById(productId);
        if (product == null) {
            throw new RuntimeException("Product not found with id: " + productId);
        }

        // 2. Tạo khóa chính phức hợp
        OrderLineId id = new OrderLineId(orderId, productId);

        // 3. Tạo và gán đầy đủ thông tin cho đối tượng OrderLine mới
        OrderLine newOrderLine = new OrderLine();
        newOrderLine.setId(id);
        newOrderLine.setOrder(order);
        newOrderLine.setProduct(product);
        newOrderLine.setAmount(orderLineDetails.getAmount());

        // Nếu giá mua không được cung cấp, lấy giá hiện tại của sản phẩm
        if (orderLineDetails.getPurchasePrice() == null) {
            newOrderLine.setPurchasePrice(BigDecimal.valueOf(product.getPrice())); // Giả sử có phương thức này trong Product
        } else {
            newOrderLine.setPurchasePrice(orderLineDetails.getPurchasePrice());
        }

        // 4. Lưu đúng đối tượng vào đúng repository
        return orderLineRepository.save(newOrderLine);
    }

    /**
     * Cập nhật số lượng của một chi tiết đơn hàng.
     */
    public Optional<OrderLine> updateAmount(OrderLineId id, int newAmount) {
        return orderLineRepository.findById(id).map(orderLine -> {
            orderLine.setAmount(newAmount);
            return orderLineRepository.save(orderLine);
        });
    }

    /**
     * Xóa một chi tiết đơn hàng.
     */
    public void deleteOrderLine(OrderLineId id) {
        orderLineRepository.deleteById(id);
    }
}
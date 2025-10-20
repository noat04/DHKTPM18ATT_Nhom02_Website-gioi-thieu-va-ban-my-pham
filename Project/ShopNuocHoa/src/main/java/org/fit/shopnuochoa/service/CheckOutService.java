package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.*;
import org.fit.shopnuochoa.repository.OrderLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckOutService {
    private final ProductService productService;
    private final OrderService orderService;
    private final CustomerService customerService;
    private final OrderLineRepository orderLineRepository;

    public CheckOutService(ProductService productService, OrderService orderService, CustomerService customerService, OrderLineRepository orderLineRepository) {
        this.productService = productService;
        this.orderService = orderService;
        this.customerService = customerService;
        this.orderLineRepository = orderLineRepository;
    }

    /**
     * Dùng để kiểm tra giỏ hàng trước khi cho phép người dùng đến trang xác nhận.
     */
    public List<String> validateCart(CartBean cart) {
        List<String> errors = new ArrayList<>();
        if (cart == null || cart.getItems().isEmpty()) {
            errors.add("Giỏ hàng đang trống.");
            return errors;
        }
        for (CartItemBean item : cart.getItems()) {
            Product productInDb = productService.getById(item.getProduct().getId());
            if (productInDb == null || !productInDb.getInStock()) {
                errors.add("Sản phẩm \"" + item.getProduct().getName() + "\" đã hết hàng hoặc không tồn tại.");
            }
        }
        return errors;
    }

    /**
     * Phương thức duy nhất thực hiện việc tạo và lưu đơn hàng.
     * Nó nhận ID và giỏ hàng, sau đó thực hiện toàn bộ quy trình trong 1 transaction.
     */
    @Transactional
    public Orders finalizeOrder(Integer customerId, CartBean cart) {
        // Lấy thông tin khách hàng
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng.");
        }

        // Tạo và LƯU đối tượng Orders TRƯỚC TIÊN để có ID
        Orders newOrder = new Orders();
        newOrder.setCustomer(customer);
        newOrder.setDate(LocalDateTime.now());
        Orders savedOrder = orderService.createOrder(newOrder);

        // Bây giờ, tạo và lưu các đối tượng OrderLine
        for (CartItemBean item : cart.getItems()) {
            Product product = productService.getById(item.getProduct().getId());
            if (product == null || !product.getInStock()) {
                throw new RuntimeException("Rất tiếc, sản phẩm \"" + item.getProduct().getName() + "\" vừa hết hàng.");
            }

            OrderLineId orderLineId = new OrderLineId(savedOrder.getId(), product.getId());
            OrderLine newOrderLine = new OrderLine();
            newOrderLine.setId(orderLineId);
            newOrderLine.setOrder(savedOrder);
            newOrderLine.setProduct(product);
            newOrderLine.setAmount(item.getQuantity());
            newOrderLine.setPurchasePrice(BigDecimal.valueOf(product.getPrice()));

            orderLineRepository.save(newOrderLine);
        }

        // Logic cập nhật tồn kho (nếu có)
        // ...

        return savedOrder;
    }
}
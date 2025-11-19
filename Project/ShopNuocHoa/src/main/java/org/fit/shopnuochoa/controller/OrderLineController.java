package org.fit.shopnuochoa.controller;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.model.OrderLine;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.OrderLineService;
import org.fit.shopnuochoa.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class OrderLineController {

    private final OrderLineService orderLineService;
    private final OrderService orderService;

    /**
     * Xem chi tiết đơn hàng (các sản phẩm trong đơn)
     * URL: /admin/orders/{orderId}/details
     */
    @GetMapping("/{orderId}/details")
    public String viewOrderDetails(@PathVariable("orderId") Integer orderId, Model model) {
        // 1. Lấy thông tin đơn hàng
        Orders order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        // 2. Lấy danh sách chi tiết đơn hàng
        Set<OrderLine> orderLines = order.getOrderLines();
        if (orderLines == null || orderLines.isEmpty()) {
            orderLines = orderLineService.findAll().stream()
                    .filter(line -> line.getOrder().getId().equals(orderId))
                    .collect(Collectors.toSet());
        }

        // 3. Gửi dữ liệu qua view
        model.addAttribute("pageTitle", "Chi tiết đơn hàng #" + orderId);
        model.addAttribute("order", order);
        model.addAttribute("orderLines", orderLines);

        // 4. Trả về trang hiển thị
        return "screen/admin/admin-order-details"; // file .html của admin
    }

    /**
     * API: Xóa một dòng sản phẩm trong đơn hàng
     * (dùng cho phần quản lý hoặc khi admin muốn xóa 1 sản phẩm trong đơn)
     */
    @PostMapping("/{orderId}/details/delete")
    public String deleteOrderLine(@PathVariable("orderId") Integer orderId,
                                  @RequestParam("productId") Integer productId) {

        // Tạo khóa tổng hợp
        orderLineService.deleteOrderLine(new org.fit.shopnuochoa.model.OrderLineId(orderId, productId));

        // Quay lại trang chi tiết đơn
        return "redirect:/admin/orders/" + orderId + "/details";
    }
}

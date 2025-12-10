package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.component.SecurityUtils;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.OrderLine;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.CustomerService;
import org.fit.shopnuochoa.service.OrderLineService;
import org.fit.shopnuochoa.service.OrderService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/orders")
public class OrderController {

    private final SecurityUtils securityUtils;
    private final OrderService orderService;
    private final OrderLineService orderLineService;
    private final UserService userService;
    private final CustomerService customerService;

    public OrderController(OrderService orderService, OrderLineService orderLineService, UserService userService, CustomerService customerService,SecurityUtils securityUtils) {
        this.orderService = orderService;
        this.orderLineService = orderLineService;
        this.userService = userService;
        this.customerService = customerService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public String showOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            Model model,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage;

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                ordersPage = orderService.searchByCustomerNameOrUsername(keyword.trim(), pageable);
            } else {
                ordersPage = orderService.findAll(pageable);
            }
        } else {
            //Customer xem đơn hàng của chính họ
            String username = authentication.getName();
            Users user = userService.getUserByUsername(username);
            if (user == null) {
                model.addAttribute("message", "Không tìm thấy người dùng trong hệ thống.");
                return "error/404";
            }

            Customer customer = customerService.getByUser(user.getId());
            if (customer == null) {
                model.addAttribute("message", "Không tìm thấy thông tin khách hàng.");
                return "error/404";
            }

            ordersPage = orderService.findByCustomer(customer.getId(), pageable);
        }

        // Lấy danh sách các đơn hàng ở trang hiện tại
        List<Orders> ordersOnPage = ordersPage.getContent();

        // Tạo Map<Integer, List<OrderLine>> mà HTML cần
        // (Giả sử bạn có hàm 'findAllByOrderId' trong OrderLineService)
        Map<Integer, List<OrderLine>> orderLinesMap = ordersOnPage.stream()
                .collect(Collectors.toMap(
                        Orders::getId, // Key = Order ID
                        order -> orderLineService.findAllByOrderId(order.getId()) // Value = List OrderLines
                ));

        // Thêm Map này vào model
        model.addAttribute("orderLinesByOrder", orderLinesMap);
        // Truyền danh sách đơn hàng sang view
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return isAdmin
                ? "screen/admin/admin-order-list"
                : "screen/customer/history-shopping";
    }
    // 3. Xử lý ĐÃ NHẬN HÀNG
    @PostMapping("/complete")
    public String completeOrder(@RequestParam("orderId") Integer orderId, RedirectAttributes ra) {
        try {
            orderService.confirmReceived(orderId);
            ra.addFlashAttribute("successMessage", "Cảm ơn bạn đã mua sắm! Đơn hàng đã hoàn tất.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi xác nhận: " + e.getMessage());
        }
        return "redirect:/api/orders/list";
    }

}

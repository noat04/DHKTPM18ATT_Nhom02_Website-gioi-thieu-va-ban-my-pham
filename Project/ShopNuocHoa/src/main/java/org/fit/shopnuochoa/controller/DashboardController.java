package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.fit.shopnuochoa.service.CustomerService;
import org.fit.shopnuochoa.service.OrderService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal; // [QUAN TRỌNG] Import BigDecimal
import java.util.ArrayList;  // [QUAN TRỌNG] Import ArrayList
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final OrdersRepository ordersRepository;
    private final UserService userService;
    private final CustomerService customerService;
    private final OrderService orderService;

    public DashboardController(OrdersRepository ordersRepository,
                               UserService userService,
                               CustomerService customerService,
                               OrderService orderService) {
        this.ordersRepository = ordersRepository;
        this.userService = userService;
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showOrders(Model model) {
        List<Orders> orders = ordersRepository.findAll();
        model.addAttribute("orders", orders);
        return "screen/admin/admin_dashboard";
    }

    @GetMapping("/test")
    public String showCustomerDashboard(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/api/login";
        }

        // 1. Lấy Customer hiện tại
        String username = authentication.getName();
        Users user = userService.getUserByUsername(username);
        Customer customer = customerService.getByUser(user.getId());

        if (customer == null) {
            return "redirect:/api/home";
        }

        // 2. Lấy danh sách đơn hàng
        // (Giả sử OrderService có hàm này, nếu chưa có hãy dùng OrderRepository.findByCustomerId)
        List<Orders> orders = orderService.getByCustomer(customer.getId());

        // 3. Tính toán số liệu thống kê
        int totalOrders = orders.size();

        // [SỬA LỖI] Dùng BigDecimal thay vì double
        BigDecimal totalSpent = BigDecimal.ZERO;

        int pendingOrders = 0;

        // [SỬA LỖI] Map lưu giá trị tiền là BigDecimal
        Map<Integer, BigDecimal> monthlySpending = new TreeMap<>();

        // Khởi tạo 12 tháng = 0
        for (int i = 1; i <= 12; i++) {
            monthlySpending.put(i, BigDecimal.ZERO);
        }

        for (Orders order : orders) {
            // [SỬA LỖI] Dùng phương thức .add() để cộng BigDecimal
            // Kiểm tra null để tránh lỗi
            if (order.getTotal() != null) {
                totalSpent = totalSpent.add(order.getTotal());

                // Dữ liệu cho biểu đồ
                int month = order.getDate().getMonthValue();
                int year = order.getDate().getYear();

                // Chỉ lấy dữ liệu năm nay
                if (year == java.time.LocalDate.now().getYear()) {
                    BigDecimal currentMonthTotal = monthlySpending.get(month);
                    monthlySpending.put(month, currentMonthTotal.add(order.getTotal()));
                }
            }
        }

        // Chuẩn bị dữ liệu gửi sang View
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("customer", customer);

        // Dữ liệu biểu đồ
        model.addAttribute("chartLabels", new ArrayList<>(monthlySpending.keySet()));
        model.addAttribute("chartData", new ArrayList<>(monthlySpending.values()));

        return "screen/customer/customer-dashboard";
    }
}
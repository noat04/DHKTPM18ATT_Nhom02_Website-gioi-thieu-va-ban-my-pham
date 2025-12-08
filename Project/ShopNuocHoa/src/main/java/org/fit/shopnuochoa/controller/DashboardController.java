package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.Enum.Role;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.fit.shopnuochoa.service.CustomerService;
import org.fit.shopnuochoa.service.OrderService;
import org.fit.shopnuochoa.service.ProductService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    private final ProductService productService;
    public DashboardController(OrdersRepository ordersRepository,
                               UserService userService,
                               CustomerService customerService,
                               OrderService orderService,
                               ProductService productService) {
        this.ordersRepository = ordersRepository;
        this.userService = userService;
        this.customerService = customerService;
        this.orderService = orderService;
        this.productService=productService;
    }
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showOrders(Model model,
                             @RequestParam(defaultValue = "0") int page) {

        int pageSize = 5;
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("date").descending());
        Page<Orders> ordersPage = ordersRepository.findAll(pageable);

        // Đếm user theo role
        long totalCustomers = userService.countByRole(Role.CUSTOMER);
        long totalAdmins =  userService.countByRole(Role.ADMIN);
        long totalUsers =  userService.count();

        // Tổng đơn hàng
        long totalOrders = ordersRepository.count();

        // Đơn hàng tuần
        long ordersThisWeek = orderService.countOrdersInWeek();

        // Đơn hàng tháng
        long ordersThisMonth = orderService.countOrdersInMonth();

        long totalProducts = productService.count();
        long inStockProducts = productService.countInStock();
        long outOfStockProducts =productService.countOutOfStock();

        BigDecimal revenueThisMonth = orderService.getMonthlyRevenue();

        // Biểu đồ 12 tháng
        List<BigDecimal> revenue12Months = orderService.getRevenue12Months();
        model.addAttribute("revenue12Months", revenue12Months);

        // Doanh thu tháng
        BigDecimal revenueThisMonths = orderService.getMonthlyRevenue();
        model.addAttribute("revenueThisMonths", revenueThisMonths);

        model.addAttribute("revenueThisMonth", revenueThisMonth);

        model.addAttribute("inStockProducts", inStockProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);


        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("ordersThisWeek", ordersThisWeek);
        model.addAttribute("ordersThisMonth", ordersThisMonth);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("totalProducts", totalProducts);

        return "screen/admin/admin_dashboard";
    }
}
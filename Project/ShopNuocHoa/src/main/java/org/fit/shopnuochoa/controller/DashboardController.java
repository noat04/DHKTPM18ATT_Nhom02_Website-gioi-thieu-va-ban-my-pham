package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.model.Role;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.fit.shopnuochoa.service.OrderService;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.fit.shopnuochoa.repository.UserRepository;
import org.fit.shopnuochoa.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final OrdersRepository ordersRepository;
    private final UserRepository usersRepository;
    private final OrderService orderService;

    private final ProductService productService;
    private final ProductRepository productRepository;

    public DashboardController(OrdersRepository ordersRepository,
                               UserRepository usersRepository,
                               OrderService orderService,
                               ProductService productService,
                               ProductRepository productRepository) {
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
        this.orderService = orderService;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showOrders(Model model,
                             @RequestParam(defaultValue = "0") int page) {

        int pageSize = 5;
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("date").descending());
        Page<Orders> ordersPage = ordersRepository.findAll(pageable);

        // Đếm user theo role
        long totalCustomers = usersRepository.countByRole(Role.CUSTOMER);
        long totalAdmins = usersRepository.countByRole(Role.ADMIN);
        long totalUsers = usersRepository.count();

        // Tổng đơn hàng
        long totalOrders = ordersRepository.count();

        // Đơn hàng tuần
        long ordersThisWeek = orderService.countOrdersInWeek();

        // Đơn hàng tháng
        long ordersThisMonth = orderService.countOrdersInMonth();

        long totalProducts = productRepository.count();
        long inStockProducts = productRepository.countByInStockTrue();
        long outOfStockProducts = productRepository.countByInStockFalse();

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

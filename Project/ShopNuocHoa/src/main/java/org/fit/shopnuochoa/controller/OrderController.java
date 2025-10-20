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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
            Model model,
            Authentication authentication) {

        // 2️⃣ Lấy user hiện tại từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Lấy username đã đăng nhập

        // 3️⃣ Tìm User & Customer tương ứng
        Users user = userService.getUserByUsername(username);
        Customer customer = customerService.getByUser(user.getId());

        Pageable pageable = PageRequest.of(page, size);
        Page<Orders> ordersPage = orderService.findByCustomer(customer.getId(), pageable);

        // Lấy toàn bộ OrderLine để hiển thị chi tiết sản phẩm trong từng đơn hàng
        List<OrderLine> allOrderLines = orderLineService.findAll();

        // Gom nhóm chi tiết theo OrderId
        Map<Integer, List<OrderLine>> orderLinesByOrder = allOrderLines.stream()
                .collect(Collectors.groupingBy(line -> line.getOrder().getId()));

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orderLinesByOrder", orderLinesByOrder);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());

        // ✅ Kiểm tra role để điều hướng đến view tương ứng
        String roleView = authentication.getAuthorities().stream()
                .anyMatch(auth1 -> auth1.getAuthority().equals("ROLE_ADMIN"))
                ? "screen/admin/admin-oder-list"
                : "screen/customer/history-shopping";
        authentication.getAuthorities().forEach(auth1 ->
                System.out.println("Current role: " + auth1.getAuthority())
        );
        return roleView;
//        return "screen/customer/history-shopping";
    }
}

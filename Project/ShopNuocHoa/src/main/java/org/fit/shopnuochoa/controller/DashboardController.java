package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/api/dashboard")
public class  DashboardController {
    private OrdersRepository ordersRepository;
    public DashboardController(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }
    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showOrders(Model model,
                             @RequestParam(defaultValue = "0") int page) {

        int pageSize = 5; // mỗi trang 5 giao dịch
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by("date").descending());

        Page<Orders> ordersPage = ordersRepository.findAll(pageable);

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent()); // để không ảnh hưởng HTML khác

        return "screen/admin/admin_dashboard";
    }


}

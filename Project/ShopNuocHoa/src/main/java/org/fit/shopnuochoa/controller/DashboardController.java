package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String showOrders(Model model) {
        List<Orders> orders = ordersRepository.findAll();
        model.addAttribute("orders", orders);
        return "screen/admin/admin_dashboard";
    }

}

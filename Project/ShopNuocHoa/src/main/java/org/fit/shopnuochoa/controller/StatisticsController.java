package org.fit.shopnuochoa.controller;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class StatisticsController {

    private final OrderService orderService;

    @GetMapping("/statistics")
    public String showStatistics(Model model) {

        // Tổng hợp số liệu
        long totalOrders = orderService.countTotalOrders();
        BigDecimal totalRevenue = orderService.getTotalRevenueYear();

        // Biểu đồ 12 tháng
        List<BigDecimal> revenue12Months = orderService.getRevenue12Months();

        // Biểu đồ 30 ngày gần nhất
        List<BigDecimal> revenue30Days = orderService.getRevenueLast30Days();
        List<String> revenueDates30Days = orderService.getLast30DaysLabels();

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("revenue12Months", revenue12Months);

        model.addAttribute("revenue30Days", revenue30Days);
        model.addAttribute("revenueDates30Days", revenueDates30Days);

        return "screen/admin/admin-statistics";
    }
}

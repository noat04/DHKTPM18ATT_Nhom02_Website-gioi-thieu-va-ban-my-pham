package org.fit.shopnuochoa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller
@RequestMapping("/api")
public class HomeController {
    @GetMapping("/home")
    public String HomePage(Model model)
    {
        LocalDate date = LocalDate.now();
        String mess ="Welcome Thymeleaf";
        model.addAttribute("message", mess);
        model.addAttribute("date", date.toString());
        return "screen/customer/home";
    }
}

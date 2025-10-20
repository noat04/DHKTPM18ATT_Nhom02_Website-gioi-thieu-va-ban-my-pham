package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.CustomerService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api")
@SessionAttributes("loggedInUser")
public class UserController {

    private final UserService userService;
    private final CustomerService customerService;
    @Autowired
    public UserController(UserService userService,CustomerService customerService) {
        this.userService = userService;
        this.customerService = customerService;
    }

    // Cung cấp một đối tượng trống cho các form binding
    @ModelAttribute("userForm")
    public Users userForm() {
        return new Users();
    }

    /**
     * HIỂN THỊ TRANG ĐĂNG NHẬP
     * Xử lý các tín hiệu từ Spring Security (?error và ?logout)
     */
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {

        // Nếu URL có tham số 'error', hiển thị thông báo lỗi
        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác.");
        }

        // Nếu URL có tham số 'logout', hiển thị thông báo đăng xuất
        if (logout != null) {
            model.addAttribute("logoutMessage", "Bạn đã đăng xuất thành công!");
        }

        return "screen/login";
    }

    // PHƯƠNG THỨC @PostMapping("/login") ĐÃ ĐƯỢC XÓA
    // VÌ SPRING SECURITY TỰ ĐỘNG XỬ LÝ VIỆC NÀY

    // Xử lý đăng xuất (giữ nguyên)
    @GetMapping("/logout")
    public String logout(HttpSession session, SessionStatus status) {
        status.setComplete();
        session.invalidate();
        return "redirect:/api/login?logout";
    }

    // Các phương thức đăng ký
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerForm", new Users());
        return "screen/customer/register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("registerForm") Users user, RedirectAttributes redirectAttributes) {
        try {
            // 1️⃣ Lưu user mới vào DB
            Users savedUser = userService.registerNewUser(user);

            // 2️⃣ Tạo customer tương ứng
            Customer newCustomer = new Customer();
            newCustomer.setName(savedUser.getFull_name()); // có thể dùng full_name làm tên KH
            customerService.createCustomer(newCustomer, savedUser.getId());

            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/api/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/api/register";
        }
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal org.springframework.security.core.userdetails.User user, Model model) {
        model.addAttribute("username", user.getUsername());
        return "screen/account-setting";
    }

}
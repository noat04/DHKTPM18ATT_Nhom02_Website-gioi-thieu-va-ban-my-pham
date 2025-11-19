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

import java.util.Optional;

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

    // ==========================
// 🔹 QUẢN LÝ TÀI KHOẢN (ADMIN)
// ==========================
    @GetMapping("/admin/users")
    public String listAllUsers(Model model) {
        model.addAttribute("users", userService.getAll());
        return "screen/admin/admin-user-list";
    }

    // Xem chi tiết tài khoản (không hiển thị mật khẩu)
    @GetMapping("/admin/users/{id}")
    public String viewUserDetails(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Users user = userService.getUserById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
            return "redirect:/api/admin/users";
        }
        model.addAttribute("user", user);
        return "screen/admin/admin-user-detail";
    }

    // Xóa tài khoản (chỉ khi chưa từng đặt hàng)
    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable int id, RedirectAttributes ra) {
        try {
            boolean deleted = userService.deleteUserById(id);
            if (deleted) {
                ra.addFlashAttribute("successMessage", "Xóa tài khoản thành công.");
            } else {
                ra.addFlashAttribute("errorMessage", "Tài khoản không tồn tại.");
            }
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/api/admin/users";
    }

    // Hiển thị form chỉnh sửa người dùng
    @GetMapping("/admin/users/update/{id}")
    public String showEditForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Users user = userService.getUserById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng!");
            return "redirect:/api/admin/users";
        }
        model.addAttribute("user", user);
        return "screen/admin/admin-user-edit"; // View đúng
    }

    // Cập nhật thông tin người dùng
    @PostMapping("/admin/users/update/{id}")
    public String updateUser(@PathVariable int id,
                             @ModelAttribute("user") Users updatedUser,
                             RedirectAttributes redirectAttributes) {
        Optional<Users> updated = userService.updateUser(id, updatedUser);
        if (updated.isPresent()) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật tài khoản!");
        }
        return "redirect:/api/admin/users";
    }
}
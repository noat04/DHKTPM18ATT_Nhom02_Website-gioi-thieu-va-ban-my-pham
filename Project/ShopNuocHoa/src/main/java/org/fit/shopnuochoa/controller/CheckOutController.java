package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.CheckOutService;
import org.fit.shopnuochoa.service.CustomerService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/api/checkout")
public class CheckOutController {

    private final CheckOutService checkoutService;
    private final CustomerService customerService;
    private final UserService userService;
    public CheckOutController(CheckOutService checkoutService,CustomerService customerService,UserService userService) {
        this.checkoutService = checkoutService;
        this.customerService = customerService;
        this.userService = userService;
    }

    // Bước 1 (POST): Xử lý Logic (Validate) -> Redirect sang GET
    @PostMapping("/confirm")
    public String processConfirm(HttpSession session, RedirectAttributes redirectAttributes) {
        CartBean cart = (CartBean) session.getAttribute("cart");

        // 1. Validate
        List<String> errors = checkoutService.validateCart(cart);
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", String.join("; ", errors));
            return "redirect:/api/cart"; // Lỗi -> Về giỏ hàng
        }

        // 2. Lấy user hiện tại (để lưu vào session cho bước sau)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Users user = userService.getUserByUsername(username);
        Customer customer = customerService.getByUser(user.getId());

        // Lưu customerId vào session
        session.setAttribute("checkoutCustomerId", customer.getId());

        // 3. [QUAN TRỌNG] Redirect sang trang hiển thị (GET)
        // Thay vì trả về view trực tiếp, ta chuyển hướng sang hàm @GetMapping("/confirm")
        return "redirect:/api/checkout/confirm";
    }
    @GetMapping("/confirm")
    public String showConfirmPage(HttpSession session, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra lại session (phòng trường hợp vào thẳng link mà ko có giỏ hàng)
        CartBean cart = (CartBean) session.getAttribute("cart");
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/api/cart";
        }

        // 2. Kiểm tra đăng nhập
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/api/login";
        }

        // 3. Lấy thông tin Customer để hiển thị
        String username = authentication.getName();
        Users user = userService.getUserByUsername(username);
        if (user == null || user.getCustomer() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin khách hàng.");
            return "redirect:/api/cart";
        }

        // 4. Đưa dữ liệu vào Model
        model.addAttribute("customer", user.getCustomer());
        model.addAttribute("cart", cart);

        return "screen/customer/checkout-confirm";
    }

    // XỬ LÝ KHI NGƯỜI DÙNG NHẤN "ĐẶT HÀNG" -> HOÀN TẤT GIAO DỊCH
    @PostMapping("/finalize")
    public String finalizeOrder(HttpSession session, RedirectAttributes redirectAttributes) {
        CartBean cart = (CartBean) session.getAttribute("cart");
        Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");

        if (cart == null || customerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn. Vui lòng thử lại.");
            return "redirect:/api/cart";
        }

        try {
            Orders finalOrder = checkoutService.finalizeOrder(customerId, cart);

            session.removeAttribute("cart");
            session.removeAttribute("checkoutCustomerId");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt hàng thành công! Mã đơn hàng của bạn là #" + finalOrder.getId());
            return "redirect:/api/checkout/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi xảy ra: " + e.getMessage());
            return "redirect:/api/checkout/confirm";
        }
    }

    @GetMapping("/success")
    public String showSuccessPage() {
        return "screen/customer/checkout-success";
    }
}
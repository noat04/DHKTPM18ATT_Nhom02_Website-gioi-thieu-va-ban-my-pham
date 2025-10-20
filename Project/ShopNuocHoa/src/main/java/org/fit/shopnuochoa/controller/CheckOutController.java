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

    // Bước 1: Người dùng nhấn "Thanh toán", kiểm tra và hiển thị trang xác nhận
    @PostMapping("/confirm")
    public String showConfirmPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        CartBean cart = (CartBean) session.getAttribute("cart");

        List<String> errors = checkoutService.validateCart(cart);
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", String.join("; ", errors));
            return "redirect:/api/cart"; // Quay về giỏ hàng báo lỗi
        }

        // 2️⃣ Lấy user hiện tại từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Lấy username đã đăng nhập

        // 3️⃣ Tìm User & Customer tương ứng
        Users user = userService.getUserByUsername(username);
        Customer customer = customerService.getByUser(user.getId());

        // Lưu customerId vào session để dùng ở bước cuối
        session.setAttribute("checkoutCustomerId", customer.getId());

        // Gửi các đối tượng cần thiết cho view để hiển thị
        model.addAttribute("cart", cart);
        model.addAttribute("customer", customer);
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
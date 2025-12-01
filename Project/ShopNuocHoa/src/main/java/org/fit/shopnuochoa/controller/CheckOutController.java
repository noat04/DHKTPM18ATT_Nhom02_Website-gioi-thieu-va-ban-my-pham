package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.Enum.PaymentMethod;
import org.fit.shopnuochoa.Enum.ShippingMethod;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.CheckOutService;
import org.fit.shopnuochoa.service.CustomerService;
import org.fit.shopnuochoa.service.EmailService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/api/checkout")
public class CheckOutController {

    private final CheckOutService checkoutService;
    private final CustomerService customerService;
    private final UserService userService;
    private final EmailService emailService;
    public CheckOutController(CheckOutService checkoutService,CustomerService customerService,UserService userService,
                              EmailService emailService) {
        this.checkoutService = checkoutService;
        this.customerService = customerService;
        this.userService = userService;
        this.emailService=emailService;
    }

    // Bước 1 (POST): Xử lý Logic (Validate) -> Redirect sang GET
    @PostMapping("/confirm")
    public String processConfirm(
            @RequestParam(value = "couponCode", required = false) String couponCode, // <-- Nhận mã từ Cart
            HttpSession session, RedirectAttributes redirectAttributes) {
        CartBean cart = (CartBean) session.getAttribute("cart");
        System.out.println("POST /api/checkout/confirm - received couponCode = " + couponCode);
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

        // [THÊM MỚI] Lưu mã giảm giá vào Session để dùng cho bước sau
        if (couponCode != null && !couponCode.isEmpty()) {
            session.setAttribute("checkoutCouponCode", couponCode);
        } else {
            session.removeAttribute("checkoutCouponCode"); // Xóa nếu không nhập
        }

        // 3. [QUAN TRỌNG] Redirect sang trang hiển thị (GET)
        // Thay vì trả về view trực tiếp, ta chuyển hướng sang hàm @GetMapping("/confirm")
        return "redirect:/api/checkout/confirm";
    }
    @GetMapping("/confirm")
    public String showConfirmPage(
            HttpSession session, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {

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

        // [THÊM MỚI] Tính toán lại số tiền giảm giá để hiển thị
        String couponCode = (String) session.getAttribute("checkoutCouponCode");
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (couponCode != null) {
            // Gọi Service để tính tiền giảm (Hàm này tôi sẽ viết ở Bước 2)
            discountAmount = checkoutService.calculateDiscountAmount(cart, couponCode, user.getCustomer().getId());
        }

        // Tính phí ship mặc định (Standard) để hiển thị ban đầu
        BigDecimal shippingFee = BigDecimal.valueOf(30000);

        // Tính tổng cuối cùng
        BigDecimal cartTotal = BigDecimal.valueOf(cart.getTotal());
        BigDecimal finalTotal = cartTotal.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);
        System.out.println(finalTotal);
        // Đẩy dữ liệu ra View
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("appliedCoupon", couponCode); // Để hiển thị lại mã vào ô input nếu cần


        // 4. Đưa dữ liệu vào Model
        model.addAttribute("customer", user.getCustomer());
        model.addAttribute("cart", cart);

        return "screen/customer/checkout-confirm";
    }

    // XỬ LÝ KHI NGƯỜI DÙNG NHẤN "ĐẶT HÀNG" -> HOÀN TẤT GIAO DỊCH
    @PostMapping("/finalize")
    public String finalizeOrder(
            @RequestParam(value = "shippingAddress", required = false) String shippingAddress,
            @RequestParam(value = "paymentMethod", defaultValue = "COD") PaymentMethod paymentMethod,
            @RequestParam(value = "shippingMethod", defaultValue = "STANDARD") ShippingMethod shippingMethod,
            @RequestParam(value = "note", required = false) String note, // <-- Nhận note
            HttpSession session, RedirectAttributes redirectAttributes) {

        CartBean cart = (CartBean) session.getAttribute("cart");
        Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");
        String couponCode = (String) session.getAttribute("checkoutCouponCode");
        if (cart == null || customerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn. Vui lòng thử lại.");
            return "redirect:/api/cart";
        }

        try {
            // ⚠ FIX THỨ TỰ THAM SỐ ĐÚNG
            Orders finalOrder = checkoutService.finalizeOrderCOD(
                    customerId,
                    cart,
                    paymentMethod,
                    shippingMethod,
                    shippingAddress,
                    note,
                    couponCode
            );

            System.out.println("Controller nhận orderLines size = " + finalOrder.getOrderLines().size());

            // Gửi mail PDF
            try {
                emailService.sendInvoiceEmailWithPdf(finalOrder);
            } catch (Exception ex) {
                System.err.println("Gửi email hóa đơn thất bại: " + ex.getMessage());
            }

            // Cleanup session
            session.removeAttribute("cart");
            session.removeAttribute("checkoutCustomerId");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt hàng thành công! Mã đơn hàng của bạn là #" + finalOrder.getId());
            return "redirect:/api/checkout/success";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đặt hàng: " + e.getMessage());
            return "redirect:/api/checkout/confirm";
        }
    }


    @GetMapping("/success")
    public String showSuccessPage() {
        return "screen/customer/checkout-success";
    }
}
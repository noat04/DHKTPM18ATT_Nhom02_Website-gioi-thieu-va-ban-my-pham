package org.fit.shopnuochoa.controller.PaymentController;

import jakarta.servlet.http.HttpSession; // <-- Import
import org.fit.shopnuochoa.service.PaymentService.VnpayService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // <-- Import
import org.springframework.web.servlet.view.RedirectView;

import lombok.AllArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.util.Map; // <-- Import

@Controller
@RequestMapping("/api/vnpayment") // <-- [SỬA LỖI 1] Sửa đường dẫn (thêm 'ment')
@AllArgsConstructor
public class VnpayController {

    private final VnpayService vnpayService;

    /**
     * [SỬA ĐỔI] Thêm HttpSession để lấy giỏ hàng
     */
    @GetMapping("/create-payment")
    public RedirectView createPayment(
            @RequestParam("amount") String amount,
            HttpSession session // <-- Thêm session
    ) {

        // Kiểm tra xem giỏ hàng và customerId có trong session không
        if (session.getAttribute("cart") == null || session.getAttribute("checkoutCustomerId") == null) {
            return new RedirectView("/api/cart?error=session_expired");
        }

        // ... (Code tạo VnpayRequest của bạn giữ nguyên)
        org.fit.shopnuochoa.dto.VnpayRequest paymentRequest = new org.fit.shopnuochoa.dto.VnpayRequest();
        paymentRequest.setAmount(amount);

        try {
            String paymentUrl = vnpayService.createPayment(paymentRequest);
            return new RedirectView(paymentUrl);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new RedirectView("/api/cart?error=payment_failed");
        }
    }

    /**
     * [SỬA LỖI BẢO MẬT]
     * Nhận TẤT CẢ tham số vào Map và gửi cho Service để xác thực
     */
    @GetMapping("/return")
    public String returnPayment(@RequestParam Map<String, String> allParams, // <-- Sửa: Nhận tất cả
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        // Gọi Service để xử lý (Service này sẽ trả về một chuỗi redirect)
        return vnpayService.handlePaymentReturn(allParams, session, redirectAttributes);
    }
}
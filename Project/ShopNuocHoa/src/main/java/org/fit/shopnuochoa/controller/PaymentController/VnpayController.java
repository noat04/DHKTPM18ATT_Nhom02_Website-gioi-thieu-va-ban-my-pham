package org.fit.shopnuochoa.controller.PaymentController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.service.PaymentService.VnpayService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import lombok.AllArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Controller
@RequestMapping("/api/vnpayment")
@AllArgsConstructor
public class VnpayController {

    private final VnpayService vnpayService;

    /**
     * [SỬA ĐỔI] Thêm HttpSession để lấy giỏ hàng
     */
    @GetMapping("/create-payment")
    public RedirectView createPayment(
            @RequestParam("amount") String amount,
            @RequestParam(required = false) String note,
            HttpSession session, // <-- Thêm session
            HttpServletRequest request // <-- Thêm tham số này
    ) {

        // Kiểm tra xem giỏ hàng và customerId có trong session không
        if (session.getAttribute("cart") == null || session.getAttribute("checkoutCustomerId") == null) {
            return new RedirectView("/api/cart?error=session_expired");
        }


        org.fit.shopnuochoa.dto.VnpayRequest paymentRequest = new org.fit.shopnuochoa.dto.VnpayRequest();
        paymentRequest.setAmount(amount);

        try {
            session.setAttribute("checkoutNote", note);
            String paymentUrl = vnpayService.createPayment(paymentRequest, request);

            return new RedirectView(paymentUrl);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new RedirectView("/api/cart?error=payment_failed");
        }
    }

    /**
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
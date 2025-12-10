package org.fit.shopnuochoa.controller.PaymentController;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.dto.MomoRequest;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.service.PaymentService.MomoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/api/momo")
public class MomoController {

    @Autowired
    private MomoService momoService;


    @PostMapping
    @ResponseBody
    public ResponseEntity<String> createPayment(@RequestBody MomoRequest paymentRequest, HttpSession session) {
        try {
            // 1. Lưu giỏ hàng và thông tin COD snapshot
            CartBean cart = (CartBean) session.getAttribute("cart");
            Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");

            if (cart == null || customerId == null) {
                return ResponseEntity.internalServerError().body("{\"message\":\"Session expired\"}");
            }

            session.setAttribute("momo_amount", paymentRequest.getAmount());
            session.setAttribute("checkout_by_momo", true);
            session.setAttribute("checkoutAddress", paymentRequest.getShippingAddress());
            session.setAttribute("checkoutNote", paymentRequest.getNote());
            session.setAttribute("checkoutShipping", paymentRequest.getShippingMethod());
            session.setAttribute("checkoutCouponCode", paymentRequest.getCouponCode());

            // 2. Gọi MoMo API lấy payUrl
            String momoJson = momoService.createPaymentRequest(paymentRequest.getAmount());
            return ResponseEntity.ok(momoJson);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"message\":\"Lỗi momo\"}");
        }
    }


    /**
     * Endpoint này (REDIRECT_URL) được MoMo gọi về
     * Nó sẽ xử lý và chuyển hướng người dùng
     */
    @GetMapping("/return")
    public String handlePaymentReturn(@RequestParam Map<String, String> allParams,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {

        // Chuyển toàn bộ logic xử lý và xác thực sang Service
        return momoService.handlePaymentReturn(allParams, session, redirectAttributes);
    }

    //Lỗi
    @GetMapping("/order-status/{orderId}")
    @ResponseBody
    public String checkPaymentStatus(@PathVariable String orderId) {
        return momoService.checkPaymentStatus(orderId);
    }

}
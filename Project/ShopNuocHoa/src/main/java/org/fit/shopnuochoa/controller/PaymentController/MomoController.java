package org.fit.shopnuochoa.controller.PaymentController;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.dto.MomoRequest;
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

//    @PostMapping
//    public String testPayment(@RequestBody MomoRequest paymentRequest) {
//        String response = momoService.createPaymentRequest(paymentRequest.getAmount());
//        return response;
//    }
    @PostMapping
    @ResponseBody // <-- Thêm @ResponseBody để trả về JSON
    public ResponseEntity<String> createPayment(@RequestBody MomoRequest paymentRequest) {
        try {
            // Service trả về JSON response (chứa payUrl) từ MoMo
            String momoResponseJson = momoService.createPaymentRequest(paymentRequest.getAmount());
            return ResponseEntity.ok(momoResponseJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

//    @GetMapping("/order-status/{orderId}")
//    public String checkPaymentStatus(@PathVariable String orderId) {
//        String response = momoService.checkPaymentStatus(orderId);
//        return response;
//    }
    /**
     * [SỬA ĐỔI]
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

    // (Hàm checkPaymentStatus của bạn giữ nguyên)
    @GetMapping("/order-status/{orderId}")
    @ResponseBody // <-- Thêm @ResponseBody
    public String checkPaymentStatus(@PathVariable String orderId) {
        return momoService.checkPaymentStatus(orderId);
    }

}
package org.fit.shopnuochoa.controller.PaymentController;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.service.PaymentService.ZalopayService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;


import java.util.Map;
import java.util.Random;

@Controller
@RequestMapping("/api/zalopay")
public class ZalopayController {

    private ZalopayService zalopayService;
    public ZalopayController(ZalopayService zalopayService) {
        this.zalopayService = zalopayService;
    }

    /**
     * [SỬA ĐỔI]
     * Chuyển thành @Controller, dùng @GetMapping và trả về RedirectView.
     */
    @GetMapping("/create-payment")
    public RedirectView createPayment(@RequestParam("amount") String amount,
                                      @RequestParam(required = false) String note,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra session
        if (session.getAttribute("cart") == null || session.getAttribute("checkoutCustomerId") == null) {
            return new RedirectView("/api/cart?error=session_expired");
        }

        try {
            session.setAttribute("checkoutNote", note);
            // 2. Chuyển đổi amount
            long amountAsLong = (long) Double.parseDouble(amount);
            String appUser = "user123"; // (Bạn có thể lấy username thật nếu muốn)

            // 3. Tạo app_trans_id duy nhất và LƯU VÀO SESSION
            // (Chúng ta cần cái này để xác thực khi ZaloPay gọi về)
            String appTransId = zalopayService.getCurrentTimeString("yyMMdd") + "_" + new Random().nextInt(1000000);
            session.setAttribute("zalopay_trans_id", appTransId);

            String orderDescription = "Thanh toan don hang #" + appTransId;

            // 4. Gọi service để lấy JSON response
            String jsonResponse = zalopayService.createOrder(amountAsLong, appUser, orderDescription, appTransId);
            JSONObject json = new JSONObject(jsonResponse);

            // 5. Kiểm tra lỗi (ví dụ: Chữ ký sai)
            if (json.getInt("return_code") != 1) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ZaloPay: " + json.getString("return_message"));
                return new RedirectView("/api/cart");
            }

            // 6. Lấy order_url và chuyển hướng người dùng
            return new RedirectView(json.getString("order_url"));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không xác định: " + e.getMessage());
            return new RedirectView("/api/cart");
        }
    }

    /**
     * [THÊM MỚI]
     * Xử lý khi ZaloPay chuyển hướng người dùng về (redirecturl trong embed_data)
     */
    @GetMapping("/return")
    public String handlePaymentReturn(@RequestParam Map<String, String> allParams,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {

        // Giao toàn bộ logic xử lý và xác thực cho Service
        return zalopayService.handlePaymentReturn(allParams, session, redirectAttributes);
    }

    @GetMapping("/order-status/{appTransId}")
    @ResponseBody // <-- Thêm
    public ResponseEntity<String> getOrderStatus(@PathVariable String appTransId) {
        String response = zalopayService.getOrderStatus(appTransId);
        return ResponseEntity.ok(response);
    }

}
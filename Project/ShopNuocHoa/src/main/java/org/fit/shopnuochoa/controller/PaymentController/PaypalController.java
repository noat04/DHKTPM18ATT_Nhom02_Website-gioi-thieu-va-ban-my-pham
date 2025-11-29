package org.fit.shopnuochoa.controller.PaymentController;

import com.paypal.base.rest.PayPalRESTException;
import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.PaymentService.PaypalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller // Đổi từ @RestController sang @Controller để redirect trang web
@RequestMapping("/api/paypal")
public class PaypalController {

    @Autowired
    private PaypalService paypalService;

    // 1. BẮT ĐẦU THANH TOÁN
    @PostMapping("/pay")
    public String createPayment(
            @RequestParam(value = "note", required = false) String note, // [THÊM] Nhận note từ form
            HttpSession session, RedirectAttributes redirectAttributes) {
        CartBean cart = (CartBean) session.getAttribute("cart");

        session.setAttribute("checkoutNote", note);
        if (cart == null || cart.getTotal() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng trống!");
            return "redirect:/api/cart";
        }

        try {
            // Lấy tổng tiền VND từ giỏ hàng
            double totalVND = cart.getTotal();

            // Tạo link thanh toán PayPal
            String approvalUrl = paypalService.createPayment(totalVND, "Thanh toan don hang ShopNuocHoa");

            // Chuyển hướng người dùng sang trang PayPal
            return "redirect:" + approvalUrl;

        } catch (PayPalRESTException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khởi tạo PayPal: " + e.getMessage());
            return "redirect:/api/cart";
        }
    }

    // 2. XỬ LÝ KHI KHÁCH THANH TOÁN THÀNH CÔNG (RETURN URL)
    @GetMapping("/success")
    public String success(@RequestParam("paymentId") String paymentId,
                          @RequestParam("PayerID") String payerId,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        try {
            // Thực hiện trừ tiền và Lưu đơn hàng vào DB
            Orders order = paypalService.executePaymentAndFinalizeOrder(paymentId, payerId, session);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán PayPal thành công! Mã đơn: #" + order.getId());

            return "redirect:/api/checkout/success";

        } catch (PayPalRESTException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý thanh toán PayPal: " + e.getMessage());
            return "redirect:/api/cart";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu đơn hàng: " + e.getMessage());
            return "redirect:/api/cart";
        }
    }

    // 3. XỬ LÝ KHI KHÁCH HỦY (CANCEL URL)
    @GetMapping("/cancel")
    public String cancel(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã hủy thanh toán PayPal.");
        return "redirect:/api/cart";
    }
}
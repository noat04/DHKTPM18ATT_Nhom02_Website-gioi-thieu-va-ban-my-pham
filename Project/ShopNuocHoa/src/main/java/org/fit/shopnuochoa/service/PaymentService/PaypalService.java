package org.fit.shopnuochoa.service.PaymentService;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.CheckOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class PaypalService {

    @Autowired
    private APIContext apiContext;

    @Autowired
    private CheckOutService checkOutService;

    @Value("${app.public.url}")
    private String NGROK_PUBLIC_URL;

    // Tỷ giá hối đoái tạm tính (1 USD = 25,000 VND)
    private static final double VND_TO_USD_RATE = 25000.0;

    public String createPayment(Double totalVND, String description) throws PayPalRESTException {

        // 1. CHUYỂN ĐỔI TIỀN TỆ (VND -> USD)
        // PayPal không nhận VND, phải chia cho tỷ giá
        double totalUSD = totalVND / VND_TO_USD_RATE;

        // Format chuẩn 2 số thập phân (Ví dụ: "10.50")
        String totalString = String.format(Locale.US, "%.2f", totalUSD);

        Amount amount = new Amount();
        amount.setCurrency("USD");
        amount.setTotal(totalString);

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = Arrays.asList(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        // 2. THIẾT LẬP URL TRẢ VỀ (DÙNG NGROK ĐỂ GIỮ SESSION)
        RedirectUrls redirectUrls = new RedirectUrls();
        // Link khi khách hủy (Cancel)
        redirectUrls.setCancelUrl(NGROK_PUBLIC_URL + "/api/paypal/cancel");
        // Link khi khách thanh toán xong (Success)
        redirectUrls.setReturnUrl(NGROK_PUBLIC_URL + "/api/paypal/success");

        payment.setRedirectUrls(redirectUrls);

        Payment createdPayment = payment.create(apiContext);

        for (Links link : createdPayment.getLinks()) {
            if (link.getRel().equals("approval_url")) {
                return link.getHref();
            }
        }

        return null;
    }

    @Transactional
    public Orders executePaymentAndFinalizeOrder(String paymentId, String payerId, HttpSession session)
            throws PayPalRESTException {

        // 1. Thực hiện trừ tiền trên PayPal
        Payment payment = new Payment();
        payment.setId(paymentId);
        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);
        Payment executedPayment = payment.execute(apiContext, paymentExecution);

        // 2. Kiểm tra nếu trạng thái là "approved" (đã thanh toán)
        if ("approved".equals(executedPayment.getState())) {

            // Lấy thông tin từ Session để lưu vào Database
            CartBean cart = (CartBean) session.getAttribute("cart");
            Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");

            if (cart == null || customerId == null) {
                throw new RuntimeException("Phiên làm việc hết hạn hoặc giỏ hàng trống.");
            }

            // 3. GỌI CHECKOUT SERVICE ĐỂ LƯU ĐƠN HÀNG
            Orders newOrder = checkOutService.finalizeOrder(customerId, cart);

            // Xóa session giỏ hàng sau khi lưu thành công
            session.removeAttribute("cart");
            session.removeAttribute("checkoutCustomerId");

            return newOrder;
        } else {
            throw new RuntimeException("Thanh toán PayPal chưa hoàn tất. Trạng thái: " + executedPayment.getState());
        }
    }
}
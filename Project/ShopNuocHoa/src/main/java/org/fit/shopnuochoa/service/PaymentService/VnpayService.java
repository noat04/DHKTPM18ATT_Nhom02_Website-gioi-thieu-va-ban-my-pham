package org.fit.shopnuochoa.service.PaymentService;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.config.VnpayConfig;
import org.fit.shopnuochoa.dto.VnpayRequest;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.CheckOutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Service
public class VnpayService {
    // === THÊM SERVICE CHECKOUT ===
    private final CheckOutService checkOutService;

    // === CẬP NHẬT CONSTRUCTOR ===
    public VnpayService(CheckOutService checkOutService) {
        this.checkOutService = checkOutService;
    }
    public String createPayment(VnpayRequest paymentRequest) throws UnsupportedEncodingException {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

//        long amount = 0;
//        try {
//            amount = Long.parseLong(paymentRequest.getAmount()) * 100;
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("Số tiền không hợp lệ");
//        }
        long amount = 0;
        try {
            // 1. Chuyển đổi chuỗi (ví dụ: "100000.0") sang số thực (Double)
            double amountAsDouble = Double.parseDouble(paymentRequest.getAmount());

            // 2. Chuyển số thực sang số nguyên (long) VÀ nhân 100
            amount = (long) amountAsDouble * 100;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }

        String bankCode = "NCB";
        String vnp_TxnRef = VnpayConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VnpayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_BankCode", bankCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnpayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append('&');
                hashData.append('&');
            }
        }

        if (query.length() > 0)
            query.setLength(query.length() - 1);
        if (hashData.length() > 0)
            hashData.setLength(hashData.length() - 1);

        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        return VnpayConfig.vnp_PayUrl + "?" + query;
    }

    /**
     * [SỬA ĐỔI HOÀN TOÀN]
     * Xử lý logic trả về TẠI ĐÂY (thay vì trong Controller).
     * Bao gồm: Xác thực Chữ ký (Hash) và Hoàn tất đơn hàng.
     */
    public String handlePaymentReturn(Map<String, String> allParams, HttpSession session, RedirectAttributes redirectAttributes) {

        // 1. Lấy SecureHash mà VNPay gửi về
        String vnp_SecureHash = allParams.get("vnp_SecureHash");

        // 2. Xóa hash khỏi map để chuẩn bị tính toán lại
        allParams.remove("vnp_SecureHash");
        allParams.remove("vnp_SecureHashType"); // (Nếu có)

        // 3. Sắp xếp các tham số theo A-Z
        List<String> fieldNames = new ArrayList<>(allParams.keySet());
        Collections.sort(fieldNames);

        // 4. Tạo chuỗi hashData (giống hệt logic createPayment)
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = allParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    hashData.append('&');
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    // Lỗi nghiêm trọng, nên báo lỗi thanh toán
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo chữ ký thanh toán.");
                    return "redirect:/api/cart";
                }
            }
        }
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }

        // 5. Tính toán chữ ký (hash) của chúng ta
        String calculatedHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());

        // 6. === KIỂM TRA BẢO MẬT ===
        if (vnp_SecureHash != null && vnp_SecureHash.equals(calculatedHash)) {

            // ----- HASH HỢP LỆ (Dữ liệu từ VNPay) -----

            String responseCode = allParams.get("vnp_ResponseCode");
            CartBean cart = (CartBean) session.getAttribute("cart");
            Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");

            if ("00".equals(responseCode)) {
                // Thanh toán THÀNH CÔNG

                if (cart == null || customerId == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn. Vui lòng thử lại.");
                    return "redirect:/api/cart";
                }

                try {
                    // GỌI LOGIC CHÍNH: Lưu đơn hàng, trừ kho
                    Orders finalOrder = checkOutService.finalizeOrder(customerId, cart);

                    session.removeAttribute("cart");
                    session.removeAttribute("checkoutCustomerId");

                    redirectAttributes.addFlashAttribute("successMessage",
                            "Thanh toán thành công! Mã đơn hàng của bạn là #" + finalOrder.getId());
                    return "redirect:/api/checkout/success"; // Chuyển đến trang success

                } catch (Exception e) {
                    // Lỗi (ví dụ: hết hàng)
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu đơn hàng: " + e.getMessage());
                    return "redirect:/api/cart";
                }

            } else {
                // Thanh toán THẤT BẠI (Bị hủy, thiếu tiền, v.v.)
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán VNPay thất bại. Mã lỗi: " + responseCode);
                return "redirect:/api/cart";
            }

        } else {
            // ----- HASH KHÔNG HỢP LỆ (Lỗi bảo mật) -----
            redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại: Chữ ký không hợp lệ.");
            return "redirect:/api/cart";
        }
    }
}
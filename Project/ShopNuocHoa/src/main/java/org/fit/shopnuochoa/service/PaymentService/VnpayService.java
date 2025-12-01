package org.fit.shopnuochoa.service.PaymentService;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.Enum.PaymentMethod;
import org.fit.shopnuochoa.Enum.ShippingMethod;
import org.fit.shopnuochoa.config.VnpayConfig;
import org.fit.shopnuochoa.dto.VnpayRequest;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.CheckOutService;
import org.fit.shopnuochoa.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Service
public class VnpayService {
    @Value("${app.public.url}")
    private String NGROK_PUBLIC_URL;

    // === THÊM SERVICE CHECKOUT ===
    private final CheckOutService checkOutService;
    private final EmailService emailService;

    // === CẬP NHẬT CONSTRUCTOR ===
    public VnpayService(CheckOutService checkOutService,EmailService emailService) {
        this.checkOutService = checkOutService;
        this.emailService=emailService;
    }
//    public String createPayment(VnpayRequest paymentRequest) throws UnsupportedEncodingException {
//        String vnp_Version = "2.1.0";
//        String vnp_Command = "pay";
//        String orderType = "other";
//
////        long amount = 0;
////        try {
////            amount = Long.parseLong(paymentRequest.getAmount()) * 100;
////        } catch (NumberFormatException e) {
////            throw new IllegalArgumentException("Số tiền không hợp lệ");
////        }
//        long amount = 0;
//        try {
//            double amountDouble = Double.parseDouble(paymentRequest.getAmount());
//            amount = (long) (amountDouble * 100);
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("Số tiền không hợp lệ");
//        }
//
//        String bankCode = "NCB";
//        String vnp_TxnRef = VnpayConfig.getRandomNumber(8);
//        String vnp_IpAddr = "127.0.0.1";
//        String vnp_TmnCode = VnpayConfig.vnp_TmnCode;
//
//        Map<String, String> vnp_Params = new HashMap<>();
//        vnp_Params.put("vnp_Version", vnp_Version);
//        vnp_Params.put("vnp_Command", vnp_Command);
//        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
//        vnp_Params.put("vnp_Amount", String.valueOf(amount));
//        vnp_Params.put("vnp_CurrCode", "VND");
//
//        vnp_Params.put("vnp_BankCode", bankCode);
//        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
//        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
//        vnp_Params.put("vnp_OrderType", orderType);
//        vnp_Params.put("vnp_Locale", "vn");
//        vnp_Params.put("vnp_ReturnUrl", VnpayConfig.vnp_ReturnUrl);
//        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
//
//        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        String vnp_CreateDate = formatter.format(cld.getTime());
//        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
//
//        cld.add(Calendar.MINUTE, 15);
//        String vnp_ExpireDate = formatter.format(cld.getTime());
//        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
//
//        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
//        Collections.sort(fieldNames);
//        StringBuilder hashData = new StringBuilder();
//        StringBuilder query = new StringBuilder();
//        for (String fieldName : fieldNames) {
//            String fieldValue = vnp_Params.get(fieldName);
//            if ((fieldValue != null) && (fieldValue.length() > 0)) {
//                hashData.append(fieldName).append('=')
//                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
//                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
//                        .append('=')
//                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
//                query.append('&');
//                hashData.append('&');
//            }
//        }
//
//        if (query.length() > 0)
//            query.setLength(query.length() - 1);
//        if (hashData.length() > 0)
//            hashData.setLength(hashData.length() - 1);
//
//        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());
//        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
//            String paymentUrl = VnpayConfig.vnp_PayUrl + "?" + query;
//    System.out.println("VNPAY URL: " + paymentUrl);
//        return VnpayConfig.vnp_PayUrl + "?" + query;
//    }
// Trong file: VnpayService.java
public String createPayment(VnpayRequest paymentRequest, HttpServletRequest request) throws UnsupportedEncodingException {
    String vnp_Version = "2.1.0";
    String vnp_Command = "pay";
    String orderType = "other";

    long amount = 0;
    try {
        double amountDouble = Double.parseDouble(paymentRequest.getAmount());
        amount = (long) (amountDouble * 100);
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Số tiền không hợp lệ");
    }

    String bankCode = "NCB";
    String vnp_TxnRef = String.valueOf(System.currentTimeMillis());
    String vnp_IpAddr = VnpayConfig.getIpAddress(request);
    String vnp_TmnCode = VnpayConfig.vnp_TmnCode;

    // 2. [SỬA LỖI] Tạo Return URL trực tiếp tại đây
    // Đảm bảo không bị null
    String vnp_ReturnUrl = NGROK_PUBLIC_URL + "/api/vnpayment/return";

    Map<String, String> vnp_Params = new HashMap<>();
    vnp_Params.put("vnp_Version", vnp_Version);
    vnp_Params.put("vnp_Command", vnp_Command);
    vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
    vnp_Params.put("vnp_Amount", String.valueOf(amount));
    vnp_Params.put("vnp_CurrCode", "VND");

    vnp_Params.put("vnp_BankCode", bankCode);
    vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
    vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
    vnp_Params.put("vnp_OrderType", orderType);
    vnp_Params.put("vnp_Locale", "vn");

    // Thêm URL vừa tạo vào Map
    vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);

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
            // Build hash data
            hashData.append(fieldName);
            hashData.append('=');
            hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            // Build query
            query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
            query.append('=');
            query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                query.append('&');
                hashData.append('&');
            }
        }
    }

    String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());
    query.append("&vnp_SecureHash=").append(vnp_SecureHash);

    String paymentUrl = VnpayConfig.vnp_PayUrl + "?" + query.toString();
    System.out.println("VNPAY URL: " + paymentUrl);

    return paymentUrl;
}
    /**
     * [SỬA ĐỔI HOÀN TOÀN]
     * Xử lý logic trả về TẠI ĐÂY (thay vì trong Controller).
     * Bao gồm: Xác thực Chữ ký (Hash) và Hoàn tất đơn hàng.
     */
//    @Transactional
//    public String handlePaymentReturn(Map<String, String> allParams, HttpSession session, RedirectAttributes redirectAttributes) {
//
//        // 1. Lấy SecureHash mà VNPay gửi về
//        String vnp_SecureHash = allParams.get("vnp_SecureHash");
//
//        // 2. Xóa hash khỏi map để chuẩn bị tính toán lại
//        allParams.remove("vnp_SecureHash");
//        allParams.remove("vnp_SecureHashType");
//
//        // 3. Sắp xếp các tham số theo A-Z
//        List<String> fieldNames = new ArrayList<>(allParams.keySet());
//        Collections.sort(fieldNames);
//
//        // 4. Tạo chuỗi hashData
//        StringBuilder hashData = new StringBuilder();
//        for (String fieldName : fieldNames) {
//            String fieldValue = allParams.get(fieldName);
//            if ((fieldValue != null) && (fieldValue.length() > 0)) {
//                try {
//                    hashData.append(fieldName).append('=')
//                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
//                    hashData.append('&');
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo chữ ký thanh toán.");
//                    return "redirect:/api/cart";
//                }
//            }
//        }
//        if (hashData.length() > 0) {
//            hashData.setLength(hashData.length() - 1);
//        }
//
//        // 5. Tính toán chữ ký (hash) của chúng ta
//        String calculatedHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());
//
//        // 6. === KIỂM TRA BẢO MẬT ===
//        if (vnp_SecureHash != null && vnp_SecureHash.equals(calculatedHash)) {
//
//            // ----- HASH HỢP LỆ (Dữ liệu từ VNPay là thật) -----
//
//            String responseCode = allParams.get("vnp_ResponseCode");
//            CartBean cart = (CartBean) session.getAttribute("cart");
//            Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");
//
//            // == TRƯỜNG HỢP 1: THANH TOÁN THÀNH CÔNG ==
//            if ("00".equals(responseCode)) {
//
//                if (cart == null || customerId == null) {
//                    redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn. Vui lòng thử lại.");
//                    return "redirect:/api/cart";
//                }
//
//                try {
//                    // GỌI LOGIC CHÍNH: Lưu đơn hàng, trừ kho
//                    Orders finalOrder = checkOutService.finalizeOrder(customerId, cart);
//
//                    session.removeAttribute("cart");
//                    session.removeAttribute("checkoutCustomerId");
//
//                    redirectAttributes.addFlashAttribute("successMessage",
//                            "Thanh toán thành công! Mã đơn hàng của bạn là #" + finalOrder.getId());
//                    return "redirect:/api/checkout/success"; // Chuyển đến trang success
//
//                } catch (Exception e) {
//                    // Lỗi (ví dụ: hết hàng trong lúc thanh toán)
//                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu đơn hàng: " + e.getMessage());
//                    return "redirect:/api/cart";
//                }
//
//            }
//
//            // == TRƯỜNG HỢP 2: THANH TOÁN THẤT BẠI (Hủy, Sai OTP...) ==
//            else {
//                // "Dịch" mã lỗi sang tiếng Việt
//                String errorMessage = getVnpayErrorMessage(responseCode);
//
//                // Hiển thị lỗi chi tiết
//                redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
//                return "redirect:/api/cart"; // Quay về giỏ hàng để thanh toán lại
//            }
//
//        } else {
//            // ----- HASH KHÔNG HỢP LỆ (Lỗi bảo mật) -----
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi bảo mật: Chữ ký không hợp lệ (Checksum failed).");
//            return "redirect:/api/cart";
//        }
//    }

    @Transactional
    public String handlePaymentReturn(Map<String, String> allParams, HttpSession session, RedirectAttributes redirectAttributes) {

        // 1. Lấy SecureHash mà VNPay gửi về
        String vnp_SecureHash = allParams.get("vnp_SecureHash");

        // 2. Xóa hash khỏi map để chuẩn bị tính toán lại
        allParams.remove("vnp_SecureHash");
        allParams.remove("vnp_SecureHashType");

        // 3. Sắp xếp các tham số theo A-Z
        List<String> fieldNames = new ArrayList<>(allParams.keySet());
        Collections.sort(fieldNames);

        // 4. Tạo chuỗi hashData
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = allParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()))
                            .append('&');
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo chữ ký thanh toán.");
                    return "redirect:/api/cart";
                }
            }
        }
        if (hashData.length() > 0) hashData.setLength(hashData.length() - 1);

        // 5. Tính toán chữ ký
        String calculatedHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());

        // 6. Kiểm tra bảo mật
        if (vnp_SecureHash == null || !vnp_SecureHash.equals(calculatedHash)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi bảo mật: Chữ ký không hợp lệ.");
            return "redirect:/api/cart";
        }

        // 7. Lấy dữ liệu từ session
        CartBean cart = (CartBean) session.getAttribute("cart");
        Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");
        String shippingAddress = (String) session.getAttribute("checkoutAddress");
        String note = (String) session.getAttribute("checkoutNote");
        ShippingMethod shippingMethod = (ShippingMethod) session.getAttribute("checkoutShipping");
        String couponCode = (String) session.getAttribute("checkoutCouponCode");
        if (cart == null || customerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn. Vui lòng thử lại.");
            return "redirect:/api/cart";
        }

        // 8. Kiểm tra kết quả thanh toán VNPay
        String responseCode = allParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) { // thành công
            try {
                // 1. Lưu đơn hàng
                Orders finalOrder = checkOutService.finalizeOrderCOD(
                        customerId,
                        cart,
                        PaymentMethod.VNPAY,
                        shippingMethod,
                        shippingAddress,
                        note,
                        couponCode
                );

                // Gửi email PDF
                try {
                    emailService.sendInvoiceEmailWithPdf(finalOrder);
                } catch (Exception ex) {
                    System.err.println("Gửi email hóa đơn thất bại: " + ex.getMessage());
                }

                // 3. Cleanup session
                session.removeAttribute("cart");
                session.removeAttribute("checkoutCustomerId");
                session.removeAttribute("checkoutAddress");
                session.removeAttribute("checkoutNote");
                session.removeAttribute("checkoutShipping");

                // 4. Thông báo thành công
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán VNPay thành công! Mã đơn hàng #" + finalOrder.getId());
                return "redirect:/api/checkout/success";

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu đơn hàng: " + e.getMessage());
                return "redirect:/api/cart";
            }
        }

        else { // thất bại
            String errorMessage = getVnpayErrorMessage(responseCode);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/api/cart";
        }
    }


    private String getVnpayErrorMessage(String responseCode) {
        switch (responseCode) {
            case "07": return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09": return "Giao dịch không thành công: Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking.";
            case "10": return "Giao dịch không thành công: Khách hàng xác thực thông tin sai quá 3 lần.";
            case "11": return "Giao dịch không thành công: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "12": return "Giao dịch không thành công: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13": return "Giao dịch không thành công: Quý khách nhập sai mật khẩu xác thực giao dịch (OTP). Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "24": return "Giao dịch không thành công: Khách hàng hủy giao dịch.";
            case "51": return "Giao dịch không thành công: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65": return "Giao dịch không thành công: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75": return "Ngân hàng thanh toán đang bảo trì.";
            case "79": return "Giao dịch không thành công: Quý khách nhập sai mật khẩu thanh toán quá số lần quy định. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "99": return "Lỗi không xác định từ VNPay.";
            default:   return "Giao dịch thất bại. Mã lỗi: " + responseCode;
        }
    }
}
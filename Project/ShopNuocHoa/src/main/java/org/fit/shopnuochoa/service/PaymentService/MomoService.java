package org.fit.shopnuochoa.service.PaymentService;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fit.shopnuochoa.Enum.PaymentMethod;
import org.fit.shopnuochoa.Enum.ShippingMethod;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.CheckOutService;
import org.fit.shopnuochoa.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Service
public class MomoService {

    @Value("${app.public.url}")
    private String NGROK_PUBLIC_URL;

    private static final String PARTNER_CODE = "MOMO";
    private static final String ACCESS_KEY = "F8BBA842ECF85";
    private static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

//     private static final String REQUEST_TYPE = "captureWallet"; quét mã qr
    private static final String REQUEST_TYPE = "payWithMethod"; // cổng thanh toán

    private String REDIRECT_URL;
    private String IPN_URL;


    private final CheckOutService checkOutService;
    private final EmailService emailService;

    @Autowired
    public MomoService(CheckOutService checkOutService,EmailService emailService) {
        this.checkOutService = checkOutService;
        this.emailService=emailService;
    }

    /**
     * [THÊM MỚI]
     * Phương thức này chạy NGAY SAU KHI @Value được tiêm vào.
     * Nó sẽ gán giá trị cho các URL động.
     */
    @PostConstruct
    public void init() {
        // 3. Xây dựng URL động (sử dụng .trim() để xóa khoảng trắng nếu lỡ có)

        //server MoMo gửi URL trả về khi thành công
        this.REDIRECT_URL = NGROK_PUBLIC_URL.trim() + "/api/momo/return";

        //server MoMo “bắn” thông báo trạng thái giao dịch
        this.IPN_URL = NGROK_PUBLIC_URL.trim() + "/api/momo/ipn-notify";

        System.out.println("--- MoMo URLs Initialized ---");
        System.out.println("MoMo REDIRECT_URL: " + this.REDIRECT_URL);
        System.out.println("MoMo IPN_URL: " + this.IPN_URL);
    }

    //hàm gửi YÊU CẦU TẠO GIAO DỊCH đến MoMo.
    public String createPaymentRequest(String amount) {
        try {
            // 1. Chuyển đổi amount (String "100000.0") sang Long (100000)
            long amountAsLong;
            try {
                amountAsLong = (long) Double.parseDouble(amount);
            } catch (NumberFormatException e) {
                return "{\"error\": \"Số tiền không hợp lệ: " + amount + "\"}";
            }

            // 2. Chuyển Long thành String (để dùng cho signature) đúng format theo yêu cầu của MoMo.
            String amountAsString = String.valueOf(amountAsLong);

            // khởi tạo requestId and orderId
            String requestId = PARTNER_CODE + new Date().getTime();
            String orderId = requestId;
            String orderInfo = "SN Mobile";
            String extraData = "";

            // khởi tạo raw signature
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    ACCESS_KEY, amountAsString, extraData, IPN_URL, orderId, orderInfo, PARTNER_CODE, REDIRECT_URL,
                    requestId, REQUEST_TYPE);

            // Chữ ký chuẩn thuật toán HMAC SHA256
            String signature = signHmacSHA256(rawSignature, SECRET_KEY);
            System.out.println("Generated Signature: " + signature);

            //Tạo JSON gửi MoMo
            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", PARTNER_CODE);
            requestBody.put("accessKey", ACCESS_KEY);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amountAsString);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", REDIRECT_URL);
            requestBody.put("ipnUrl", IPN_URL);
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", REQUEST_TYPE);
            requestBody.put("signature", signature);
            requestBody.put("lang", "en");

            //Gửi HTTP POST đến MoMo
            CloseableHttpClient httpClient = HttpClients.createDefault();
            //Tạo HTTP POST request
            HttpPost httpPost = new HttpPost("https://test-payment.momo.vn/v2/gateway/api/create");
            //Set header kiểu JSON
            httpPost.setHeader("Content-Type", "application/json");
            //Gắn JSON body vào request
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            //Gửi request và nhận response
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                //Đọc nội dung trả về với
                //response.getEntity().getContent() → lấy InputStream chứa body JSON từ MoMo.
                //InputStreamReader chuyển InputStream thành dạng có thể đọc được.
                //BufferedReader đọc từng dòng cho nhanh và tiết kiệm bộ nhớ.
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));

                //Lưu toàn bộ body response
                StringBuilder result = new StringBuilder();
                String line;
                //Đọc từng dòng JSON trong response.
                while ((line = reader.readLine()) != null) {
                    //Ghép vào StringBuilder để tạo một string hoàn chỉnh.
                    result.append(line);
                }
                System.out.println("Response from MoMo: " + result.toString());

                //Trả JSON đó về controller
                return result.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to create payment request: " + e.getMessage() + "\"}";
        }
    }

    // Hàm HMAC SHA256
    private String signHmacSHA256(String data, String key) throws Exception {
        //Mac là class xử lý các dạng mã hóa kiểu MAC (Message Authentication Code)
        Mac mac = Mac.getInstance("HmacSHA256");

        //khóa để tạo signature
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        //Khởi tạo thuật toán ký với khóa bí mật
        mac.init(secretKey);

        //Tạo mã hash từ dữ liệu cần ký với mac.doFinal() chạy thuật toán HMAC-SHA256 trên data.
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        //Đổi mảng byte → chuỗi hex MoMo yêu cầu chữ ký dạng hexadecimal lowercase (00a3ffbc…)()
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        //Tạo StringBuilder để tạo chuỗi nhanh
        StringBuilder result = new StringBuilder();

        //Duyệt từng byte trong mảng
        for (byte b : bytes) {
            //Chuyển từng byte sang dạng hex
            result.append(String.format("%02x", b));
        }
        //kết quả thu được 34ab9f12ddea1a33f2bc8e9a20d8cd7b1e4f54bd... (MoMo sẽ dùng signature này để xác thực giao dịch.)
        return result.toString();
    }


    @Transactional
    public String handlePaymentReturn(Map<String, String> allParams, HttpSession session, RedirectAttributes redirectAttributes) {

        // 1. Lấy chữ ký từ MoMo
        String momoSignature = allParams.get("signature");

        // 2.Phương thức nhận vào allParams (Map chứa tất cả các tham số trên URL).
        String amount = allParams.get("amount");
        String extraData = allParams.get("extraData");
        String message = allParams.get("message");
        String orderId = allParams.get("orderId");
        String orderInfo = allParams.get("orderInfo");
        String orderType = allParams.get("orderType");
        String partnerCode = allParams.get("partnerCode");
        String payType = allParams.get("payType");
        String requestId = allParams.get("requestId");
        String responseTime = allParams.get("responseTime");
        String resultCode = allParams.get("resultCode");
        String transId = allParams.get("transId");

        // 3. Xây dựng raw signature đúng thứ tự A-Z (MoMo yêu cầu chữ ký phải được tạo từ chuỗi có thứ tự key theo thứ tự bảng chữ cái)
        StringBuilder rawSignature = new StringBuilder();
        rawSignature.append("accessKey=").append(ACCESS_KEY)
                .append("&amount=").append(amount)
                .append("&extraData=").append(extraData)
                .append("&message=").append(message)
                .append("&orderId=").append(orderId)
                .append("&orderInfo=").append(orderInfo)
                .append("&orderType=").append(orderType)
                .append("&partnerCode=").append(partnerCode)
                .append("&payType=").append(payType)
                .append("&requestId=").append(requestId)
                .append("&responseTime=").append(responseTime)
                .append("&resultCode=").append(resultCode)
                .append("&transId=").append(transId);

        try {
            // 4. Tự tính lại signature server-side để so sánh
            String calculatedSignature = signHmacSHA256(rawSignature.toString(), SECRET_KEY);

            if (momoSignature == null || !momoSignature.equals(calculatedSignature)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại: chữ ký không hợp lệ");
                return "redirect:/api/cart";
            }

            // 5. Lấy thông tin từ session
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

            //"0" là thành công
            if ("0".equals(resultCode)) {
                // Thanh toán MoMo thành công → gọi finalizeOrderCOD
                Orders finalOrder = checkOutService.finalizeOrderCOD(
                        customerId,
                        cart,
                        PaymentMethod.MOMO,
                        shippingMethod,
                        shippingAddress,
                        note,
                        couponCode
                );

                System.out.println("Momo return: orderLines size = " + finalOrder.getOrderLines().size());

                // Gửi email PDF
                try {
                    emailService.sendInvoiceEmailWithPdf(finalOrder);
                } catch (Exception ex) {
                    System.err.println("Gửi email hóa đơn thất bại: " + ex.getMessage());
                }

                // Cleanup session
                session.removeAttribute("cart");
                session.removeAttribute("checkoutCustomerId");
                session.removeAttribute("checkoutAddress");
                session.removeAttribute("checkoutNote");
                session.removeAttribute("checkoutShipping");

                //Thông báo và điều hướng
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán MoMo thành công! Mã đơn hàng #" + finalOrder.getId());
                return "redirect:/api/checkout/success";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán MoMo thất bại: " + message);
                return "redirect:/api/cart";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý thanh toán: " + e.getMessage());
            return "redirect:/api/cart";
        }
    }

    //Kiểm tra các trạng thái trả về của resultCode
    public String checkPaymentStatus(String orderId) {
        try {
            // Generate requestId
            String requestId = PARTNER_CODE + new Date().getTime();

            // Generate raw signature for the status check
            String rawSignature = String.format(
                    "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                    ACCESS_KEY, orderId, PARTNER_CODE, requestId);

            // Sign with HMAC SHA256
            String signature = signHmacSHA256(rawSignature, SECRET_KEY);
            System.out.println("Generated Signature for Status Check: " + signature);

            // Prepare request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("partnerCode", PARTNER_CODE);
            requestBody.put("accessKey", ACCESS_KEY);
            requestBody.put("requestId", requestId);
            requestBody.put("orderId", orderId);
            requestBody.put("signature", signature);
            requestBody.put("lang", "en");

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://test-payment.momo.vn/v2/gateway/api/query");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                System.out.println("Response from MoMo (Status Check): " + result.toString());
                return result.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to check payment status: " + e.getMessage() + "\"}";
        }
    }

}

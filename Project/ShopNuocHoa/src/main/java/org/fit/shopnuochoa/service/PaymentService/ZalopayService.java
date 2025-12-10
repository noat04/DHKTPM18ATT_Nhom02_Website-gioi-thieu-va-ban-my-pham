package org.fit.shopnuochoa.service.PaymentService;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.fit.shopnuochoa.Enum.PaymentMethod;
import org.fit.shopnuochoa.Enum.ShippingMethod;
import org.fit.shopnuochoa.config.ZalopayConfig;
import org.fit.shopnuochoa.cryto.HMACUtil;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.CheckOutService;
import org.fit.shopnuochoa.service.EmailService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ZalopayService {

    @Value("${app.public.url}")
    private String NGROK_PUBLIC_URL;

    private String REDIRECT_URL;
    private String CALLBACK_URL;

    private final CheckOutService checkOutService;
    private final EmailService emailService;

    @Autowired
    public ZalopayService(CheckOutService checkOutService,EmailService emailService) {
        this.checkOutService = checkOutService;
        this.emailService = emailService;
    }

    /**
     * Hàm này tự động chạy sau khi @Value được tiêm vào,
     * để gán các URL động.
     */

    @PostConstruct
    public void init() {
        this.REDIRECT_URL = NGROK_PUBLIC_URL.trim() + "/api/zalopay/return";
        this.CALLBACK_URL = NGROK_PUBLIC_URL.trim() + "/api/zalopay/callback";

        System.out.println("--- ZaloPay URLs Initialized ---");
        System.out.println("ZaloPay REDIRECT_URL: " + this.REDIRECT_URL);
        System.out.println("ZaloPay CALLBACK_URL: " + this.CALLBACK_URL);
    }

    public static String getCurrentTimeString(String format) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+7"));
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setCalendar(cal);
        return fmt.format(cal.getTimeInMillis());
    }

    //tạo đơn ZaloPay
    public String createOrder(long amount, String appUser, String orderDescription, String appTransId) {

        // 1. Chuyển 'amount' (long) sang 'String'
        String amountAsString = String.valueOf(amount);

        // 2. Tạo 'embed_data' (ĐÃ SỬA)
        // Dùng URL ngrok (công khai) thay vì localhost
        Map<String, String> embedData = new HashMap<>();
//        embedData.put("redirecturl", " https://2c05e243471f.ngrok-free.app/api/zalopay/return");
        embedData.put("redirecturl", this.REDIRECT_URL);
        String embedDataStr = new JSONObject(embedData).toString();

        // 3. Tạo 'item' (chi tiết đơn hàng) .Tạo cứng do môi trường test
        // (ZaloPay yêu cầu 'item' phải là một chuỗi JSON)
        //[{ "itemid": "product123", "itemname": "product", "itemprice": 10000, "itemquantity": 1 }]
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("itemid", "product123");
        item.put("itemname", orderDescription); // Dùng mô tả đơn hàng
        item.put("itemprice", amount); // Dùng số tiền
        item.put("itemquantity", 1);
        items.add(item);

        // Chuyển List Map thành chuỗi JSON
        String itemStr = new org.json.JSONArray(items).toString();

        // 4. Tạo Map chứa thông tin đơn hàng
        Map<String, Object> order = new HashMap<>();
        order.put("app_id", ZalopayConfig.config.get("app_id"));
        order.put("app_trans_id", appTransId);
        order.put("app_time", System.currentTimeMillis());
        order.put("app_user", appUser);
        order.put("amount", amountAsString); // Dùng Long cho chữ ký (như docs)
        order.put("description", orderDescription);
        order.put("bank_code", "ATM");
        order.put("item", itemStr); // <-- Dùng chuỗi JSON item
        order.put("embed_data", embedDataStr);
//        order.put("callback_url", " https://2c05e243471f.ngrok-free.app/api/zalopay/callback");
        order.put("callback_url", this.CALLBACK_URL); // <-- Dùng biến động

        // 5. Tạo chữ ký MAC
        String data = order.get("app_id") + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|"
                + order.get("amount") + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|"
                + order.get("item");
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, ZalopayConfig.config.get("key1"), data);
        order.put("mac", mac);

        System.out.println("ZaloPay Request Data: " + data);
        System.out.println("Generated MAC: " + mac);

        // 4. Gửi request POST
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            //Gửi đến endpoint chính thức
            HttpPost post = new HttpPost(ZalopayConfig.config.get("endpoint"));

            //Đổ các tham số vào dạng form-urlencoded (ZaloPay yêu cầu dữ liệu gửi lên theo application/x-www-form-urlencoded)
            //Mỗi cặp key-value được chuyển thành: key=value
            List<NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : order.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }

            post.setEntity(new UrlEncodedFormEntity(params));

            //Gửi request và nhận response
            try (CloseableHttpResponse response = client.execute(post)) {
                //Đọc dữ liệu JSON trả về từ ZaloPay
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                //Ghép JSON trả về thành 1 chuỗi
                StringBuilder resultJsonStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultJsonStr.append(line);
                }

                System.out.println("Zalopay Response: " + resultJsonStr.toString());

                return resultJsonStr.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"return_code\": -1, \"return_message\": \"Failed to create order: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Xử lý khi ZaloPay chuyển hướng người dùng về /return
     */

    @Transactional
    public String handlePaymentReturn(Map<String, String> allParams, HttpSession session, RedirectAttributes redirectAttributes) {

        // 1. Lấy trạng thái từ ZaloPay
        //?appid=2554&apptransid=240921_123456&status=1&...
        String status = allParams.get("status");
        String appTransId = allParams.get("apptransid");

        // 2. Lấy app_trans_id đã lưu từ session
        String sessionTransId = (String) session.getAttribute("zalopay_trans_id");

        // 3. Kiểm tra (app_trans_id phải khớp)
        if (sessionTransId == null || !sessionTransId.equals(appTransId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Mã giao dịch ZaloPay không khớp.");
            return "redirect:/api/cart";
        }

        // 4. Kiểm tra trạng thái thanh toán
        if ("1".equals(status)) { // status=1 là THÀNH CÔNG

            CartBean cart = (CartBean) session.getAttribute("cart");
            Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");
            String shippingAddress = (String) session.getAttribute("checkoutAddress");
            String note = (String) session.getAttribute("checkoutNote");
            ShippingMethod shippingMethod = (ShippingMethod) session.getAttribute("checkoutShipping");
            String couponCode = (String) session.getAttribute("checkoutCouponCode");
            if (cart == null || customerId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn.");
                return "redirect:/api/cart";
            }

            try {
                // 5. Gọi finalizeOrderCOD giống VNPay/MoMo
                Orders finalOrder = checkOutService.finalizeOrderCOD(
                        customerId,
                        cart,
                        PaymentMethod.ZALOPAY, // Thanh toán online qua ZaloPay
                        shippingMethod,
                        shippingAddress,
                        note,
                        couponCode
                );

                // 6. Gửi email hóa đơn (nếu có EmailService)
                try {
                    emailService.sendInvoiceEmailWithPdf(finalOrder);
                } catch (Exception ex) {
                    System.err.println("Gửi email hóa đơn thất bại: " + ex.getMessage());
                }

                // 7. Cleanup session
                session.removeAttribute("cart");
                session.removeAttribute("checkoutCustomerId");
                session.removeAttribute("checkoutAddress");
                session.removeAttribute("checkoutNote");
                session.removeAttribute("checkoutShipping");
                session.removeAttribute("zalopay_trans_id");

                // 8. Thông báo thành công
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán ZaloPay thành công! Mã đơn hàng #" + finalOrder.getId());
                return "redirect:/api/checkout/success";

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu đơn hàng: " + e.getMessage());
                return "redirect:/api/cart";
            }
        } else {
            // Thanh toán thất bại hoặc bị hủy
            redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán ZaloPay thất bại hoặc bị hủy.");
            return "redirect:/api/cart";
        }
    }


    public String getOrderStatus(String appTransId) {
        String data = ZalopayConfig.config.get("app_id") + "|" + appTransId + "|" + ZalopayConfig.config.get("key1");
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, ZalopayConfig.config.get("key1"), data);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(ZalopayConfig.config.get("orderstatus"));

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("app_id", ZalopayConfig.config.get("app_id")));
            params.add(new BasicNameValuePair("app_trans_id", appTransId));
            params.add(new BasicNameValuePair("mac", mac));

            post.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder resultJsonStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultJsonStr.append(line);
                }

                return resultJsonStr.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to get order status: " + e.getMessage() + "\"}";
        }
    }

}
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
import org.fit.shopnuochoa.config.ZalopayConfig;
import org.fit.shopnuochoa.cryto.HMACUtil;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.service.CheckOutService;
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

    // [THÊM MỚI] Tiêm giá trị từ application.properties
    @Value("${app.public.url}")
    private String NGROK_PUBLIC_URL;

    private String REDIRECT_URL;
    private String CALLBACK_URL;

    private final CheckOutService checkOutService;

    @Autowired // Đảm bảo bạn đã inject
    public ZalopayService(CheckOutService checkOutService) {
        this.checkOutService = checkOutService;
    }

    /**
     * [THÊM MỚI]
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

    public String createOrder(long amount, String appUser, String orderDescription, String appTransId) {

        // 1. Chuyển 'amount' (long) sang 'String'
        String amountAsString = String.valueOf(amount);

        // 2. Tạo 'embed_data' (ĐÃ SỬA)
        // Dùng URL ngrok (công khai) thay vì localhost
        Map<String, String> embedData = new HashMap<>();
//        embedData.put("redirecturl", " https://2c05e243471f.ngrok-free.app/api/zalopay/return");
        embedData.put("redirecturl", this.REDIRECT_URL);
        String embedDataStr = new JSONObject(embedData).toString();

        // 3. [SỬA LỖI] Tạo 'item' (chi tiết đơn hàng)
        // (ZaloPay yêu cầu 'item' phải là một chuỗi JSON)
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
        // 5. Tạo chữ ký MAC (Giữ nguyên)
        String data = order.get("app_id") + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|"
                + order.get("amount") + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|"
                + order.get("item");
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, ZalopayConfig.config.get("key1"), data);
        order.put("mac", mac);

        System.out.println("ZaloPay Request Data: " + data);
        System.out.println("Generated MAC: " + mac);

        // 4. Gửi request POST (Giữ nguyên)
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(ZalopayConfig.config.get("endpoint"));

            List<NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : order.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }

            post.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
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
        String status = allParams.get("status");
//        String appTransId = allParams.get("app_trans_id");
        String appTransId = allParams.get("apptransid"); // <-- ĐÃ SỬA

        // 2. Lấy app_trans_id đã lưu từ session
        String sessionTransId = (String) session.getAttribute("zalopay_trans_id");

        // 3. Kiểm tra (app_trans_id phải khớp, phòng trường hợp giả mạo)
        if (sessionTransId == null || !sessionTransId.equals(appTransId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Mã giao dịch ZaloPay không khớp.");
            return "redirect:/api/cart";
        }

        // (Trong thực tế, bạn PHẢI gọi API "getOrderStatus" ở đây
        // và xác thực chữ ký MAC trả về để đảm bảo 100% an toàn)

        // 4. Kiểm tra trạng thái thanh toán
        if ("1".equals(status)) { // status=1 là THÀNH CÔNG

            CartBean cart = (CartBean) session.getAttribute("cart");
            Integer customerId = (Integer) session.getAttribute("checkoutCustomerId");

            if (cart == null || customerId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn.");
                return "redirect:/api/cart";
            }

            try {
                // 5. GỌI LOGIC CHÍNH: Lưu đơn hàng, trừ kho
                Orders finalOrder = checkOutService.finalizeOrder(customerId, cart);

                // 6. Xóa session
                session.removeAttribute("cart");
                session.removeAttribute("checkoutCustomerId");
                session.removeAttribute("zalopay_trans_id");

                redirectAttributes.addFlashAttribute("successMessage", "Thanh toán ZaloPay thành công! Mã đơn hàng: #" + finalOrder.getId());
                return "redirect:/api/checkout/success";

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu đơn hàng: " + e.getMessage());
                return "redirect:/api/cart";
            }
        } else {
            // Thanh toán thất bại (status=2) hoặc đang chờ (status=3)
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
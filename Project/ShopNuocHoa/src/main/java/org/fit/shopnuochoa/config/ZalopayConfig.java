package org.fit.shopnuochoa.config;

import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ZalopayConfig {

    public static final Map<String, String> config = new HashMap<String, String>() {
        {
            put("app_id", "2554"); //mã định danh duy nhất của merchant khi tích hợp ZaloPay Gateway (được cấp)
            put("key1", "sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn"); //(Khóa bí mật dùng để ký (MAC))
            put("key2", "trMrHtvjo6myautxDUiAcYsVtaeQ8nhf"); //Dùng để xác minh callback từ ZaloPay (IPN)
            put("endpoint", "https://sb-openapi.zalopay.vn/v2/create"); //endpoint – API tạo đơn hàng (Create Order)
            put("orderstatus", "https://sb-openapi.zalopay.vn/v2/query"); //orderstatus – API kiểm tra trạng thái đơn hàng

        }
    };

}
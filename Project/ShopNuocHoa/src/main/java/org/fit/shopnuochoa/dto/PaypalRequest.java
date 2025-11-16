package org.fit.shopnuochoa.dto;

import lombok.Data;

@Data
public class PaypalRequest {
    private String total;
    private String currency = "USD";
}
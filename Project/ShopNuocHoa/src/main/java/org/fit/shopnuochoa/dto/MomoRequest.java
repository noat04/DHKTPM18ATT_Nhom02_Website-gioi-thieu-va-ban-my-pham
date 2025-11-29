package org.fit.shopnuochoa.dto;

import lombok.Data;
import org.fit.shopnuochoa.Enum.ShippingMethod;

@Data
public class MomoRequest {
    private String amount;
    private String shippingAddress;
    private String note;
    private ShippingMethod shippingMethod;
    private String couponCode;
    // getter setter
}

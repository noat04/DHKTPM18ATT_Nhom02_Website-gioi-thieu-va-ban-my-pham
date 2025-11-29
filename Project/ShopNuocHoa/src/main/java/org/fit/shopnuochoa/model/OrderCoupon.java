package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("ORDER_TOTAL") // Giá trị lưu trong cột 'coupon_type'
@Getter
@Setter
@NoArgsConstructor
public class OrderCoupon extends Coupon {

    // Chỉ loại này mới cần trường này
    private BigDecimal minOrderAmount;

    @Override
    public boolean isApplicable(CartBean cart, Integer customerId) {
        // Logic kiểm tra: Tổng đơn hàng >= minOrderAmount
        if (minOrderAmount == null) return true;

        // [SỬA LỖI] Chuyển đổi double -> BigDecimal để so sánh
        BigDecimal cartTotal = BigDecimal.valueOf(cart.getTotal());

        return cartTotal.compareTo(this.minOrderAmount) >= 0;
    }
}
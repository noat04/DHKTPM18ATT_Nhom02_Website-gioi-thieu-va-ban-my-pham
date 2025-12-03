package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero; // [IMPORT]
import lombok.*;
import org.fit.shopnuochoa.service.OrderService;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("ORDER_TOTAL")
@Getter
@Setter
@NoArgsConstructor
public class OrderCoupon extends Coupon {

    @PositiveOrZero(message = "Giá trị đơn tối thiểu không được âm") // Validate
    private BigDecimal minOrderAmount;

    @Override
    public boolean isApplicable(CartBean cart, Integer customerId, OrderService orderService) {
        if (minOrderAmount == null) return true;

        BigDecimal cartTotal = BigDecimal.valueOf(cart.getTotal());
        return cartTotal.compareTo(this.minOrderAmount) >= 0;
    }
}
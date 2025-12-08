package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Vui lòng nhập giá trị tối thiểu đơn hàng")
    @DecimalMin(value = "0", message = "Không được nhỏ hơn 0")
    private BigDecimal minOrderAmount;

    @Override
    public boolean isApplicable(CartBean cart, Integer customerId, OrderService orderService) {
        if (minOrderAmount == null) return true;

        BigDecimal cartTotal = BigDecimal.valueOf(cart.getTotal());
        return cartTotal.compareTo(this.minOrderAmount) >= 0;
    }
}
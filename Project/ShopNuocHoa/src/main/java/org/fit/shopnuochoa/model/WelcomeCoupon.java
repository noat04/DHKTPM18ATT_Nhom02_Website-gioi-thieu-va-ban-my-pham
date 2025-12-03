package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;
import org.fit.shopnuochoa.service.OrderService;

@Entity
@DiscriminatorValue("WELCOME")
@Getter
@Setter
@NoArgsConstructor
public class WelcomeCoupon extends Coupon {

    // Loại này không có trường dữ liệu riêng cần Validate
    // Logic validate nằm ở hàm isApplicable

    @Override
    public boolean isApplicable(CartBean cart, Integer customerId, OrderService orderService) {
        if (customerId == null || orderService == null) return false;

        try {
            long count = orderService.countByCustomerId(customerId);
            return count == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
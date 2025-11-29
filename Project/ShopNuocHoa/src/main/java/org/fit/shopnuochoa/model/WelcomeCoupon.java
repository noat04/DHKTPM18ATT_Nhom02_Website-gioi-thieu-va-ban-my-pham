package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("WELCOME")
@Getter
@Setter
@NoArgsConstructor
public class WelcomeCoupon extends Coupon {

    // Có thể thêm trường specific nếu cần, hoặc chỉ cần class này là đủ để đánh dấu logic

    @Override
    public boolean isApplicable(CartBean cart, Integer customerId) {
        // Logic này phức tạp hơn (check DB), nên thường xử lý ở Service,
        // nhưng về mặt mô hình thì class này đại diện cho Welcome Coupon.
        return true;
    }
}
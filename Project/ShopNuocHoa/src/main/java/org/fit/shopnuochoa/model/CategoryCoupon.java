package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("CATEGORY_SPECIFIC")
@Getter
@Setter
@NoArgsConstructor
public class CategoryCoupon extends Coupon {

    // Chỉ loại này mới cần liên kết Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_category_id")
    private Category targetCategory;

    @Override
    public boolean isApplicable(CartBean cart, Integer customerId) {
        // Logic kiểm tra: Trong giỏ hàng phải có sản phẩm thuộc category này
        return cart.getItems().stream()
                .anyMatch(item -> item.getProduct().getCategory().getId().equals(targetCategory.getId()));
    }
}
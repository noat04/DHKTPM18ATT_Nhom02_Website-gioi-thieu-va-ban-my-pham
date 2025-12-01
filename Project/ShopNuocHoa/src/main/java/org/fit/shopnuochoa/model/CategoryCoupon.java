package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull; // [IMPORT]
import lombok.*;
import org.fit.shopnuochoa.service.OrderService;

@Entity
@DiscriminatorValue("CATEGORY_SPECIFIC")
@Getter
@Setter
@NoArgsConstructor
public class CategoryCoupon extends Coupon {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_category_id")
    @NotNull(message = "Vui lòng chọn danh mục áp dụng") // Validate
    private Category targetCategory;

    @Override
    public boolean isApplicable(CartBean cart, Integer customerId, OrderService orderService) {
        if (targetCategory == null) return false; // Thêm check null an toàn

        return cart.getItems().stream()
                .anyMatch(item -> item.getProduct() != null &&
                        item.getProduct().getCategory() != null &&
                        item.getProduct().getCategory().getId().equals(targetCategory.getId()));
    }
}
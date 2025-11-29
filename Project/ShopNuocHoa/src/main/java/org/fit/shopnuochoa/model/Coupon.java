package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "coupons")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Chiến lược 1 bảng duy nhất
@DiscriminatorColumn(name = "coupon_type", discriminatorType = DiscriminatorType.STRING) // Cột phân loại
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    private String imageUrl;

    // Thông số giảm giá chung
    private BigDecimal discountValue;

    private boolean isPercentage;

    private BigDecimal maxDiscountAmount;

    // Quản lý
    private LocalDate startDate;

    private LocalDate endDate;

    private int quantity;

    private boolean active = true;

    @Transient
    public String getImagePath() {
        // 1. Nếu chưa có ảnh -> Trả về ảnh voucher mặc định
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "/images/default-coupon.jpg"; // Bạn nhớ tạo file này trong static/images
        }
        // 2. Nếu là ảnh Cloudinary hoặc link web -> Trả về nguyên gốc
        return imageUrl;
    }

    /**
     * Phương thức trừu tượng: Mỗi loại coupon con phải tự định nghĩa cách kiểm tra điều kiện
     */
    public abstract boolean isApplicable(CartBean cart, Integer customerId);
}
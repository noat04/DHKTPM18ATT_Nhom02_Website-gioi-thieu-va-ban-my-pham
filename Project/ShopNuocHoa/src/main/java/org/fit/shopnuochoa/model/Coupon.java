package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*; // [IMPORT QUAN TRỌNG]
import lombok.*;
import org.fit.shopnuochoa.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "coupons")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "coupon_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(min = 3, max = 50, message = "Mã giảm giá phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã giảm giá chỉ được chứa chữ hoa và số (VD: SALE2024)")
    private String code;

    @Size(max = 255, message = "Mô tả không được quá 255 ký tự")
    private String description;

    private String imageUrl;

    // Thông số giảm giá chung
    @NotNull(message = "Giá trị giảm không được để trống")
    @Positive(message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    // Cờ hiệu nếu giảm giá theo tiền mặt thì là false, giảm % là true
    private boolean isPercentage;

    @Positive(message = "Số tiền giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount;

    // Quản lý thời gian
    private LocalDate startDate;

    @FutureOrPresent(message = "Ngày kết thúc phải là hiện tại hoặc tương lai")
    private LocalDate endDate;

    @Min(value = 0, message = "Số lượng mã không được âm")
    private int quantity;

    private boolean active = true;

    // --- CUSTOM VALIDATION (Kiểm tra Logic chéo) ---

    @AssertTrue(message = "Ngày bắt đầu phải trước ngày kết thúc")
    public boolean isDateRangeValid() {
        // Nếu 1 trong 2 ngày null thì bỏ qua (để @NotNull xử lý nếu cần)
        if (startDate == null || endDate == null) return true;
        return !startDate.isAfter(endDate); // startDate <= endDate
    }

    @AssertTrue(message = "Nếu giảm theo %, giá trị phải từ 1 đến 100")
    public boolean isPercentageValid() {
        if (!isPercentage || discountValue == null) return true;
        // Nếu là %, discountValue phải <= 100
        return discountValue.compareTo(new BigDecimal("100")) <= 0;
    }

    // --- HELPER METHODS ---

    @Transient
    public String getImagePath() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "/images/default-coupon.jpg";
        }
        return imageUrl;
    }

    public abstract boolean isApplicable(CartBean cart, Integer customerId, OrderService orderService);

    @Transient
    public String getCouponTypeLabel() {
        if (this instanceof OrderCoupon) return "ORDER";
        if (this instanceof CategoryCoupon) return "CATEGORY";
        if (this instanceof WelcomeCoupon) return "WELCOME";
        return "UNKNOWN";
    }
}
package org.fit.shopnuochoa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;
//    private Boolean inStock;

    // 🟡 Mới thêm
    @Column(name = "hot_trend")
    private Boolean hotTrend = false;

    @Column(name = "rating_count") // <-- Ánh xạ tới cột 'rating_count' trong    DB
    private Integer ratingCount;   // <-- Thêm thuộc tính này

    @Column(name = "quantity") // Ánh xạ tới cột 'quantity' mới
    private Integer quantity = 0; // Đặt giá trị mặc định

    // Ảnh chính của sản phẩm
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Transient
    public String getImagePath() {
        // 1. Nếu chưa có ảnh -> Trả về ảnh mặc định
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "/images/default-product.jpg"; // Bạn nhớ tạo file này trong static/images
        }
        // 2. Nếu là ảnh Cloudinary (bắt đầu bằng http) hoặc đường dẫn hợp lệ -> Trả về nguyên gốc
        return imageUrl;
    }

    /**
     * Ánh xạ tới cột 'volume' trong DB.
     * Lưu dưới dạng String (VD: "ML_50", "ML_100")
     * thay vì số (0, 1) để an toàn khi thay đổi enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "volume")
    private Volume volume;

    /**
     * Ánh xạ tới cột 'gender' trong DB.
     * Lưu dưới dạng String (VD: "NAM", "NU", "UNISEX")
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    // Nhiều sản phẩm thuộc 1 danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonManagedReference
    private List<Comment> comments;

    @OneToMany(mappedBy = "product")
    @ToString.Exclude
    private Set<OrderLine> orderLines;

    @Transient
    public boolean isInStock() {
        return this.quantity > 0;
    }

    @Transient // Không lưu vào database
    private boolean isFavorite; // <-- thêm thuộc tính này

}


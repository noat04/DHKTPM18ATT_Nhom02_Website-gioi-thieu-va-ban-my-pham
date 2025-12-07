package org.fit.shopnuochoa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*; // [IMPORT QUAN TRỌNG]
import lombok.*;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;

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

    // 1. Validate Tên: Không được rỗng, độ dài vừa phải, ký tự hợp lệ, phải có ít nhất 1 chữ cái
    @Column(nullable = false)
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-zA-ZàáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵđÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴĐ])[a-zA-Z0-9àáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵđÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴĐ\\s\\-\\.,'&()]+$",
        message = "Tên sản phẩm phải chứa ít nhất một chữ cái và chỉ được chứa chữ cái, số, khoảng trắng và các ký tự: - . , ' & ( )"
    )
    private String name;

    // 2. Validate Giá: Không null, phải lớn hơn 0 (không chấp nhận giá = 0)
    @Column(nullable = false)
    @NotNull(message = "Giá sản phẩm không được để trống")
    @Min(value = 1, message = "Giá sản phẩm phải lớn hơn 0")
    private Double price;

    // 3. Validate Rating: Từ 0 đến 5 (Giữ nguyên Double)
    @Column(name = "average_rating")
    @Min(value = 0, message = "Điểm đánh giá thấp nhất là 0")
    @Max(value = 5, message = "Điểm đánh giá cao nhất là 5")
    private Double averageRating = 0.0;

    @Column(name = "hot_trend")
    private Boolean hotTrend = false;

    @Column(name = "rating_count")
    @Min(value = 0, message = "Lượt đánh giá không được âm")
    private Integer ratingCount;

    // 4. Validate Số lượng: Không null, phải lớn hơn 0 (không chấp nhận 0)
    @Column(name = "quantity")
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity = 0;

    // 5. Validate Ảnh: Giới hạn độ dài URL
    @Column(name = "image_url", length = 512)
    @Size(max = 512, message = "Đường dẫn ảnh quá dài (tối đa 512 ký tự)")
    private String imageUrl;

    @Transient
    public String getImagePath() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "/images/default-product.jpg";
        }
        return imageUrl;
    }

    // 6. Validate Enum Volume: Bắt buộc chọn
    @Enumerated(EnumType.STRING)
    @Column(name = "volume")
    @NotNull(message = "Vui lòng chọn dung tích")
    private Volume volume;

    // 7. Validate Enum Gender: Bắt buộc chọn
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    @NotNull(message = "Vui lòng chọn giới tính phù hợp")
    private Gender gender;

    // 8. Validate Category: Bắt buộc chọn danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @NotNull(message = "Vui lòng chọn danh mục cho sản phẩm")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonManagedReference
    private List<Comment> comments;

    @OneToMany(mappedBy = "product")
    @ToString.Exclude
    private Set<OrderLine> orderLines;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Transient
    public boolean isInStock() {
        // Kiểm tra null an toàn vì quantity là Integer (Wrapper class)
        return this.quantity != null && this.quantity > 0;
    }

    @Transient
    private boolean isFavorite;
}
package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "country", length = 100)
    private String country;

    /**
     * Ánh xạ tới cột 'imgURL' (VARCHAR(512))
     */
    @Column(name = "imgURL", length = 512)
    private String imgURL;

    @Transient
    public String getImagePath() {
        // 1. Nếu chưa có ảnh -> Trả về ảnh mặc định
        if (imgURL == null || imgURL.isEmpty()) {
            return "/images/default-product.jpg"; // Bạn nhớ tạo file này trong static/images
        }
        // 2. Nếu là ảnh Cloudinary (bắt đầu bằng http) hoặc đường dẫn hợp lệ -> Trả về nguyên gốc
        return imgURL;
    }

    // Quan hệ 1-N: 1 Category có nhiều Product
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Product> products;
}

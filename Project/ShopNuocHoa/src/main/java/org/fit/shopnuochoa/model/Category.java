package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    // 1. Kiểm tra Tên danh mục
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Tên danh mục không được để trống") // Không cho phép null hoặc chuỗi rỗng ""
    @Size(min = 2, max = 255, message = "Tên danh mục phải từ 2 đến 255 ký tự")
    private String name;

    // 2. Kiểm tra Quốc gia
    @Column(name = "country", length = 100)
    @NotBlank(message = "Quốc gia không được để trống") // Không cho phép null hoặc chuỗi rỗng ""
    @Size(max = 100, message = "Tên quốc gia không được vượt quá 100 ký tự")
    private String country;

    // 3. Kiểm tra URL ảnh
    @Column(name = "imgURL", length = 512)
    @Size(max = 512, message = "Đường dẫn ảnh quá dài (tối đa 512 ký tự)")
    private String imgURL;

    @Transient
    public String getImagePath() {
        if (imgURL == null || imgURL.isEmpty()) {
            return "/images/default-product.jpg";
        }
        return imgURL;
    }

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Product> products;
}
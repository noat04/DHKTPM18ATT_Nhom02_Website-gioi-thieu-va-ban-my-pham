package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;

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
    //BigDecimal là một đối tượng được thiết kế để xử lý các phép toán số học với độ chính xác cao (thường dùng cho tiền tệ), vì vậy bạn phải sử dụng các phương thức của chính nó để tính toán.

    private Boolean inStock;
    // Quan hệ N-1: nhiều Product thuộc 1 Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private Category category;

    // Quan hệ 1-N: 1 Product có nhiều Comment
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Comment> comments;

    // Quan hệ 1-N tới bảng trung gian OrderLine
    @OneToMany(mappedBy = "product")
    @ToString.Exclude
    private Set<OrderLine> orderLines;
}
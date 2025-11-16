package org.fit.shopnuochoa.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String text;

    // ⭐ Số sao đánh giá (1 đến 5)
    @Min(1)
    @Max(5)
    @Column(nullable = true)
    private int rating;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    // === KẾT THÚC THÊM MỚI ===

    // Quan hệ N-1: nhiều Comment thuộc về 1 Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @JsonBackReference
    private Product product;

    /**
     * Nhiều comment thuộc về 1 Customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false) // Một bình luận phải thuộc về 1 khách hàng
    @ToString.Exclude
    @JsonBackReference
    private Customer customer;

}
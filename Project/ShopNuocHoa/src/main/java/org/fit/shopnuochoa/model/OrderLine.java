// OrderLine.java
package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderLine {

    @EmbeddedId // Đánh dấu đây là khóa chính phức hợp
    private OrderLineId id;

    // Liên kết tới Order
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("orderId") // Ánh xạ thuộc tính orderId trong OrderLineId tới khóa ngoại này
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    private Orders order;

    // Liên kết tới Product
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId") // Ánh xạ thuộc tính productId trong OrderLineId tới khóa ngoại này
    @JoinColumn(name = "product_id")
    @ToString.Exclude
    private Product product;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private BigDecimal purchasePrice;
}
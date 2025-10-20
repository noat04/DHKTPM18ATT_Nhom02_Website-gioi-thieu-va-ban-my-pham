package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDateTime date;

    // Quan hệ N-1: nhiều Order thuộc về 1 Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    // Quan hệ 1-N: 1 Order có nhiều OrderLine
    // orphanRemoval=true: nếu một OrderLine bị xóa khỏi Set này, nó cũng sẽ bị xóa khỏi DB
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<OrderLine> orderLines;

    @Transient // Annotation này báo cho JPA biết không cần map thuộc tính này vào CSDL
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (orderLines == null) {
            return total;
        }
        for (OrderLine line : orderLines) {
            // Lấy giá tại thời điểm mua (purchasePrice) nhân với số lượng
            BigDecimal lineTotal = line.getPurchasePrice().multiply(new BigDecimal(line.getAmount()));
            total = total.add(lineTotal);
        }
        return total;
    }
}
    package org.fit.shopnuochoa.model;

    import jakarta.persistence.*;
    import lombok.*;
    import org.fit.shopnuochoa.Enum.OrderStatus;
    import org.fit.shopnuochoa.Enum.PaymentMethod;
    import org.fit.shopnuochoa.Enum.ShippingMethod;

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
        private LocalDateTime date; // Ngày đặt hàng

        @Column(name = "shipping_address", nullable = false, length = 500)
        private String shippingAddress;

        @Column(name = "phone_number", length = 20)
        private String phoneNumber;

        @Column(name = "note", length = 1000)
        private String note;

        @Enumerated(EnumType.STRING)
        @Column(name = "payment_method", nullable = false, length = 20)
        private PaymentMethod paymentMethod;

        @Column(name = "payment_status")
        private Boolean paymentStatus = false;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private OrderStatus status = OrderStatus.PENDING;

        @Column(name = "delivery_date")
        private LocalDateTime deliveryDate; // Ngày giao THỰC TẾ

        @Column(name = "discount_amount")
        private BigDecimal discountAmount = BigDecimal.ZERO;

        @Enumerated(EnumType.STRING)
        @Column(name = "shipping_method")
        private ShippingMethod shippingMethod = ShippingMethod.STANDARD; // Mặc định Giao chuẩn

        @Column(name = "shipping_fee")
        private BigDecimal shippingFee = BigDecimal.ZERO;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "customer_id", nullable = false)
        @ToString.Exclude
        private Customer customer;

        @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
        @ToString.Exclude
        private Set<OrderLine> orderLines;

        // --- CÁC HÀM TÍNH TOÁN (@Transient) ---

        /**
         * 1. Tính Tổng tiền hàng (Chưa ship, chưa giảm giá)
         * (Đổi tên từ getTotal cũ cho rõ nghĩa hơn, nhưng giữ getTotal để tương thích code cũ nếu cần)
         */
        @Transient
        public BigDecimal getSubTotal() {
            BigDecimal subTotal = BigDecimal.ZERO;
            if (orderLines != null) {
                for (OrderLine line : orderLines) {
                    BigDecimal lineTotal = line.getPurchasePrice().multiply(new BigDecimal(line.getAmount()));
                    subTotal = subTotal.add(lineTotal);
                }
            }
            return subTotal;
        }

        // Giữ lại tên getTotal() để tương thích với code cũ đang gọi order.getTotal()
        // Nhưng logic bên trong gọi getSubTotal()
        @Transient
        public BigDecimal getTotal() {
            return getSubTotal();
        }

        /**
         * 2. Tính Tổng thanh toán cuối cùng (Final Total)
         * = (Tiền hàng + Ship) - Giảm giá
         */
        @Transient
        public BigDecimal getFinalTotal() {
            BigDecimal subTotal = getSubTotal();
            BigDecimal ship = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;

            return subTotal.add(ship).subtract(discount).max(BigDecimal.ZERO);
        }

        /**
         * 3. [THÊM MỚI] Tính Thời gian giao hàng DỰ KIẾN
         * Logic:
         * - Hỏa tốc: +1 ngày (nội thành), +2 ngày (ngoại thành)
         * - Thường: +2 ngày (nội thành), +4 ngày (ngoại thành)
         */
        @Transient
        public LocalDateTime getEstimatedDeliveryDate() {
            LocalDateTime orderDate = (date != null ? date : LocalDateTime.now());
            boolean isExpress = (shippingMethod == ShippingMethod.EXPRESS);

            String addr = (shippingAddress != null ? shippingAddress.toLowerCase() : "");
            boolean isInnerCity = addr.contains("quận") || addr.contains("tp");

            if (isInnerCity) {
                return isExpress ? orderDate.plusDays(1) : orderDate.plusDays(3);
            } else {
                return isExpress ? orderDate.plusDays(2) : orderDate.plusDays(5);
            }
        }
    }
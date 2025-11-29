package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.OrderLine;
import org.fit.shopnuochoa.model.OrderLineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, OrderLineId> {
    /**
     * [THÊM MỚI]
     * Tự động tìm tất cả các OrderLine theo trường 'order.id'
     */
    List<OrderLine> findByOrderId(Integer orderId);
}
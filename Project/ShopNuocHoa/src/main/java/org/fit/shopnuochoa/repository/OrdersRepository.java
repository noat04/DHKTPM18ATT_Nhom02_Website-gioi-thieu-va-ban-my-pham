package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {
    Page<Orders> findByCustomerId(Integer customerId, Pageable pageable);
    Page<Orders> findByDate(LocalDate date, Pageable pageable);
    @Query("SELECT e FROM Orders e WHERE e.date BETWEEN :dateStart AND :dateEnd")
    Page<Orders> findByDateRange(LocalDateTime dateStart,
                                 LocalDateTime dateEnd,
                                 Pageable pageable);

}

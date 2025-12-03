package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.Enum.OrderStatus;
import org.fit.shopnuochoa.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    List<Orders> findByCustomerId(Integer customerId);

    Page<Orders> findByCustomerId(Integer customerId, Pageable pageable);

    Page<Orders> findByDate(LocalDate date, Pageable pageable);

    @Query("SELECT e FROM Orders e WHERE e.date BETWEEN :dateStart AND :dateEnd")
    Page<Orders> findByDateRange(LocalDateTime dateStart,
                                 LocalDateTime dateEnd,
                                 Pageable pageable);

    @Query("""
    SELECT o FROM Orders o
    JOIN o.customer c
    LEFT JOIN c.user u
    WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Orders> searchByCustomerNameOrUsername(String keyword, Pageable pageable);

    List<Orders> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Orders o JOIN FETCH o.orderLines WHERE o.id = :id")
    Orders findFullOrderWithLines(@Param("id") Integer id);

    long countByCustomerId(Integer customerId);

    @Query("SELECT COUNT(o) FROM Orders o WHERE FUNCTION('YEARWEEK', o.date) = FUNCTION('YEARWEEK', CURRENT_DATE)")
    long countOrdersInCurrentWeek();

    @Query("SELECT COUNT(o) FROM Orders o WHERE MONTH(o.date) = MONTH(CURRENT_DATE) AND YEAR(o.date) = YEAR(CURRENT_DATE)")
    long countOrdersInCurrentMonth();

    @Query("""
            SELECT o FROM Orders o 
            WHERE MONTH(o.date) = MONTH(CURRENT_DATE)
              AND YEAR(o.date) = YEAR(CURRENT_DATE)
       """)
    List<Orders> findOrdersInCurrentMonth();

    @Query("""
        SELECT COALESCE(SUM(
            (ol.purchasePrice * ol.amount) + o.shippingFee - o.discountAmount
        ), 0)
        FROM Orders o
        JOIN o.orderLines ol
        WHERE DATE(o.date) = :day
    """)
    BigDecimal getRevenueByDay(LocalDate day);

}

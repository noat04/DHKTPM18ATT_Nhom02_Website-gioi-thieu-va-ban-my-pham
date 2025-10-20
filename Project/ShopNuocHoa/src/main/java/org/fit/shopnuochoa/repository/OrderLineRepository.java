package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.OrderLine;
import org.fit.shopnuochoa.model.OrderLineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, OrderLineId> {
}
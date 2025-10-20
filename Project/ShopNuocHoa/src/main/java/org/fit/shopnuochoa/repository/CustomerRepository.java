package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Customer findByUserId(Integer userId);
}

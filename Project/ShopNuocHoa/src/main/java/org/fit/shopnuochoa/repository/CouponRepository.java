package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {
    Coupon findByCode(String code);
}

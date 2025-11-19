package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

    // [SỬA ĐỔI] Đổi tên: findByUserId... -> findByCustomerId...
    Optional<Wishlist> findByCustomerIdAndProductId(Integer customerId, Integer productId);

    // [SỬA ĐỔI] Đổi tên: existsByUserId... -> existsByCustomerId...
    boolean existsByCustomerIdAndProductId(Integer customerId, Integer productId);
}
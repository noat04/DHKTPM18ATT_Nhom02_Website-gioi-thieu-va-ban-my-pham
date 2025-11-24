package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

    Optional<Wishlist> findByCustomerIdAndProductId(Integer customerId, Integer productId);

    boolean existsByCustomerIdAndProductId(Integer customerId, Integer productId);

    List<Wishlist> findAllByCustomerId(Integer customerId);
}
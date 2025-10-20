package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Tìm employee theo departmentId
    Page<Product> findByCategoryId(Integer categoryId, Pageable pageable);
    Page<Product> findByPriceGreaterThan(Double price, Pageable pageable);

    @Query("SELECT e FROM Product e WHERE e.category.id=:categoryId and e.price > :price")
    Page<Product> findProductsByCategoryWithPriceGreaterThan(@Param("categoryId") Integer categoryId, @Param("price") Double price, Pageable pageable);

    // Tìm các sản phẩm theo categoryId và Id không phải là productId được cung cấp
    List<Product> findByCategoryIdAndIdNot(Integer categoryId, Integer productId);
}

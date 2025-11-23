package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Object> findByName(String name);

    @Query("SELECT DISTINCT c.country FROM Category c WHERE c.country IS NOT NULL AND c.country <> ''")
    List<String> findDistinctCountries();

    // Tìm kiếm theo tên với phân trang (không phân biệt hoa thường)
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}

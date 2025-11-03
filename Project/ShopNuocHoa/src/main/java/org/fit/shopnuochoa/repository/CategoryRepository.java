package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Object> findByName(String name);
}

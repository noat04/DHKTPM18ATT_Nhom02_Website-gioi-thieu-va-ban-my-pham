package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {
    Users findByUsername(String username);
    Optional<Users> findByEmail(String email); // Thêm dòng này

    @Query("""
        SELECT u FROM Users u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Users> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

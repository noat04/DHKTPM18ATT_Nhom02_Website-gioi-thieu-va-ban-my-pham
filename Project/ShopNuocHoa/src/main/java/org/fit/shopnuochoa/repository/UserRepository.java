package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {
    Users findByUsername(String username);
    Optional<Users> findByEmail(String email); // Thêm dòng này
}

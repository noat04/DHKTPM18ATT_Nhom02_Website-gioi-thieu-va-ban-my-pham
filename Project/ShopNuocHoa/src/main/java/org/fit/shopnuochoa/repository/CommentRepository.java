package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    Page<Comment> findByProductId(Integer productId, Pageable pageable);

}

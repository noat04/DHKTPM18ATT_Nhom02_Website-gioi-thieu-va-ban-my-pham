package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    Page<Comment> findByProductId(Integer productId, Pageable pageable);

    @Query("SELECT AVG(c.rating) FROM Comment c WHERE c.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Integer productId);

}

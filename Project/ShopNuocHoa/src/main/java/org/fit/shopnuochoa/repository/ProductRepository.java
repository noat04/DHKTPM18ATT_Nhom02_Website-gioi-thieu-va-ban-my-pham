package org.fit.shopnuochoa.repository;

import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;
import org.fit.shopnuochoa.dto.RatingStats;
import org.fit.shopnuochoa.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Tìm employee theo departmentId
    Page<Product> findByCategoryId(Integer categoryId, Pageable pageable);

    Page<Product> findByCategoryCountry(String categoryCountry, Pageable pageable);

    long countByCategoryCountry(String categoryCountry);

    List<Double> countProductByAverageRating(Double averageRating);

    Page<Product> findByPriceGreaterThan(Double price, Pageable pageable);

    @Query("SELECT e FROM Product e WHERE e.category.id=:categoryId and e.price > :price")
    Page<Product> findProductsByCategoryWithPriceGreaterThan(@Param("categoryId") Integer categoryId, @Param("price") Double price, Pageable pageable);

    // Tìm các sản phẩm theo categoryId và Id không phải là productId được cung cấp
    List<Product> findByCategoryIdAndIdNot(Integer categoryId, Integer productId);


    @Query("SELECT p FROM Product p WHERE "
            + "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:categoryId IS NULL OR p.category.id = :categoryId) "
            + "AND (:price IS NULL OR p.price >= :price) "
            + "AND (:maxPrice IS NULL OR p.price <= :maxPrice) "
            + "AND (:countries IS NULL OR p.category.country IN :countries) "
            + "AND (:volume IS NULL OR p.volume = :volume) "
            + "AND (:gender IS NULL OR p.gender = :gender) "

            // [SỬA LỖI] Đổi p.ratingCount thành p.averageRating
            + "AND (:rating IS NULL OR p.averageRating >= :rating)")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("price") Double price,
            @Param("maxPrice") Double maxPrice,
            @Param("countries") Collection<String> countries,
            @Param("volume") Volume volume,
            @Param("gender") Gender gender,
            @Param("rating") Double rating,
            Pageable pageable);
//    @Query("SELECT p FROM Product p WHERE "
//            + "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
//            + "AND (:categoryId IS NULL OR p.category.id = :categoryId) "
//            + "AND (:price IS NULL OR p.price >= :price)")
//    Page<Product> searchProducts(@Param("keyword") String keyword,
//                                 @Param("categoryId") Integer categoryId,
//                                 @Param("price") Double price,
//                                 Pageable pageable);

    @Query("""
        SELECT new org.fit.shopnuochoa.dto.RatingStats(
            AVG(c.rating),\s
            COUNT(c.id)
        )\s
        FROM Comment c\s
        WHERE c.product.id = :productId\s
        AND c.rating > 0
   \s""")
    RatingStats getRatingStatsByProductId(Integer productId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantity > 0")
    long countInStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantity = 0")
    long countOutOfStock();

}

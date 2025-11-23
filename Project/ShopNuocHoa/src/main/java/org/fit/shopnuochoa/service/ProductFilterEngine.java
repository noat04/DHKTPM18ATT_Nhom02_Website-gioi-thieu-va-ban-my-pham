package org.fit.shopnuochoa.service;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structured Filter Engine
 * Lọc sản phẩm dựa trên các tiêu chí có cấu trúc
 */
@Service
@RequiredArgsConstructor
public class ProductFilterEngine {

    private final ProductRepository productRepository;

    /**
     * Filter products based on structured criteria
     */
    public List<Product> filterProducts(FilterCriteria criteria) {
        List<Product> products = productRepository.findAll();

        // Apply filters
        if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
            String keyword = criteria.getKeyword().toLowerCase();
            products = products.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword) ||
                           p.getCategory().getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (criteria.getCategoryName() != null) {
            products = products.stream()
                .filter(p -> p.getCategory().getName().equalsIgnoreCase(criteria.getCategoryName()))
                .collect(Collectors.toList());
        }

        if (criteria.getGender() != null) {
            products = products.stream()
                .filter(p -> p.getGender() == criteria.getGender())
                .collect(Collectors.toList());
        }

        if (criteria.getVolume() != null) {
            products = products.stream()
                .filter(p -> p.getVolume() == criteria.getVolume())
                .collect(Collectors.toList());
        }

        if (criteria.getMinPrice() != null) {
            products = products.stream()
                .filter(p -> p.getPrice() >= criteria.getMinPrice())
                .collect(Collectors.toList());
        }

        if (criteria.getMaxPrice() != null) {
            products = products.stream()
                .filter(p -> p.getPrice() <= criteria.getMaxPrice())
                .collect(Collectors.toList());
        }

        if (criteria.getMinRating() != null) {
            products = products.stream()
                .filter(p -> p.getAverageRating() != null &&
                           p.getAverageRating() >= criteria.getMinRating())
                .collect(Collectors.toList());
        }

        if (criteria.getInStockOnly() != null && criteria.getInStockOnly()) {
            products = products.stream()
                .filter(Product::isInStock)
                .collect(Collectors.toList());
        }

        if (criteria.getHotTrendOnly() != null && criteria.getHotTrendOnly()) {
            products = products.stream()
                .filter(p -> p.getHotTrend() != null && p.getHotTrend())
                .collect(Collectors.toList());
        }

        // Apply sorting
        products = applySorting(products, criteria.getSortBy());

        // Apply limit
        if (criteria.getLimit() != null && criteria.getLimit() > 0) {
            products = products.stream()
                .limit(criteria.getLimit())
                .collect(Collectors.toList());
        }

        return products;
    }

    private List<Product> applySorting(List<Product> products, String sortBy) {
        if (sortBy == null) sortBy = "popular";

        return switch (sortBy.toLowerCase()) {
            case "price_asc" -> products.stream()
                .sorted((a, b) -> Double.compare(a.getPrice(), b.getPrice()))
                .collect(Collectors.toList());
            case "price_desc" -> products.stream()
                .sorted((a, b) -> Double.compare(b.getPrice(), a.getPrice()))
                .collect(Collectors.toList());
            case "rating" -> products.stream()
                .sorted((a, b) -> {
                    double ratingA = a.getAverageRating() != null ? a.getAverageRating() : 0;
                    double ratingB = b.getAverageRating() != null ? b.getAverageRating() : 0;
                    return Double.compare(ratingB, ratingA);
                })
                .collect(Collectors.toList());
            case "popular" -> products.stream()
                .sorted((a, b) -> {
                    int countA = a.getRatingCount() != null ? a.getRatingCount() : 0;
                    int countB = b.getRatingCount() != null ? b.getRatingCount() : 0;
                    return Integer.compare(countB, countA);
                })
                .collect(Collectors.toList());
            case "newest" -> products.stream()
                .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());
            default -> products;
        };
    }

    /**
     * Build filter criteria from user query and intents
     */
    public FilterCriteria buildCriteriaFromIntents(String query, Map<String, Object> intents) {
        FilterCriteria criteria = new FilterCriteria();

        // Set gender
        if (intents.containsKey("gender")) {
            try {
                criteria.setGender(Gender.valueOf((String) intents.get("gender")));
            } catch (Exception e) {
                // Ignore
            }
        }

        // Set price range
        if (intents.containsKey("maxPrice")) {
            criteria.setMaxPrice((Double) intents.get("maxPrice"));
        }
        if (intents.containsKey("minPrice")) {
            criteria.setMinPrice((Double) intents.get("minPrice"));
        }

        // Set brand/category
        if (intents.containsKey("brand")) {
            criteria.setCategoryName((String) intents.get("brand"));
        }

        // Default: only in-stock products
        criteria.setInStockOnly(true);

        // Default: top 10 results
        criteria.setLimit(10);

        // Sorting based on intent
        if ((Boolean) intents.getOrDefault("isPriceQuery", false)) {
            criteria.setSortBy("price_asc");
        } else if ((Boolean) intents.getOrDefault("isRecommendation", false)) {
            criteria.setSortBy("popular");
        } else {
            criteria.setSortBy("rating");
        }

        return criteria;
    }

    /**
     * Inner class for filter criteria
     */
    public static class FilterCriteria {
        private String keyword;
        private String categoryName;
        private Gender gender;
        private Volume volume;
        private Double minPrice;
        private Double maxPrice;
        private Double minRating;
        private Boolean inStockOnly;
        private Boolean hotTrendOnly;
        private String sortBy;
        private Integer limit;

        // Getters and setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public Gender getGender() { return gender; }
        public void setGender(Gender gender) { this.gender = gender; }

        public Volume getVolume() { return volume; }
        public void setVolume(Volume volume) { this.volume = volume; }

        public Double getMinPrice() { return minPrice; }
        public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

        public Double getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

        public Double getMinRating() { return minRating; }
        public void setMinRating(Double minRating) { this.minRating = minRating; }

        public Boolean getInStockOnly() { return inStockOnly; }
        public void setInStockOnly(Boolean inStockOnly) { this.inStockOnly = inStockOnly; }

        public Boolean getHotTrendOnly() { return hotTrendOnly; }
        public void setHotTrendOnly(Boolean hotTrendOnly) { this.hotTrendOnly = hotTrendOnly; }

        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }
}


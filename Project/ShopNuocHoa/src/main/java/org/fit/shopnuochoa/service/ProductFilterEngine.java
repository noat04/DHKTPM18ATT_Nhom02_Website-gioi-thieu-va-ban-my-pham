package org.fit.shopnuochoa.service;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.stereotype.Service;

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
        int initialCount = products.size();

        System.out.println("╔════════════════════════════════════════════════════════════════");
        System.out.println("║ 🔍 PRODUCT FILTERING DEBUG");
        System.out.println("╠════════════════════════════════════════════════════════════════");
        System.out.println("║ 📊 Initial products from DB: " + initialCount);
        System.out.println("╠════════════════════════════════════════════════════════════════");
        System.out.println("║ 📋 Filter Criteria:");
        System.out.println("║   • productName: " + criteria.getProductName());
        System.out.println("║   • categoryName (brand): " + criteria.getCategoryName());
        System.out.println("║   • gender: " + criteria.getGender());
        System.out.println("║   • volume: " + criteria.getVolume());
        System.out.println("║   • minPrice: " + criteria.getMinPrice());
        System.out.println("║   • maxPrice: " + criteria.getMaxPrice());
        System.out.println("║   • minRating: " + criteria.getMinRating());
        System.out.println("║   • inStockOnly: " + criteria.getInStockOnly());
        System.out.println("║   • hotTrendOnly: " + criteria.getHotTrendOnly());
        System.out.println("║   • sortBy: " + criteria.getSortBy());
        System.out.println("║   • limit: " + criteria.getLimit());
        System.out.println("╚════════════════════════════════════════════════════════════════");

        // Apply filters
        // Filter by product name (specific search with fuzzy matching)
        if (criteria.getProductName() != null && !criteria.getProductName().isEmpty()) {
            String searchTerm = criteria.getProductName().toLowerCase();
            products = products.stream()
                .filter(p -> {
                    String productName = p.getName().toLowerCase();
                    // Exact match or contains
                    if (productName.contains(searchTerm)) {
                        return true;
                    }
                    // Fuzzy matching - check if words match
                    String[] searchWords = searchTerm.split("\\s+");
                    String[] productWords = productName.split("\\s+");

                    // Count matching words
                    int matchCount = 0;
                    for (String searchWord : searchWords) {
                        for (String productWord : productWords) {
                            // Check if words are similar (contains or levenshtein distance)
                            if (productWord.contains(searchWord) || searchWord.contains(productWord)) {
                                matchCount++;
                                break;
                            }
                            // Simple typo tolerance
                            if (Math.abs(productWord.length() - searchWord.length()) <= 2 &&
                                calculateSimilarity(productWord, searchWord) > 0.6) {
                                matchCount++;
                                break;
                            }
                        }
                    }

                    // If more than 50% of search words match, consider it a match
                    return matchCount >= (searchWords.length * 0.5);
                })
                .collect(Collectors.toList());
        }

        // Filter by keyword (general search - name or category)
        if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
            String keyword = criteria.getKeyword().toLowerCase();
            products = products.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword) ||
                           p.getCategory().getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (criteria.getCategoryName() != null) {
            int beforeFilter = products.size();
            products = products.stream()
                .filter(p -> p.getCategory().getName().equalsIgnoreCase(criteria.getCategoryName()))
                .collect(Collectors.toList());
            System.out.println("║ ✂️ After brand filter: " + products.size() + " (removed: " + (beforeFilter - products.size()) + ")");
            if (products.isEmpty()) {
                System.out.println("║ ❌ NO PRODUCTS LEFT after brand filter!");
                System.out.println("║    Looking for brand: [" + criteria.getCategoryName() + "]");
            }
        }

        if (criteria.getGender() != null) {
            int beforeFilter = products.size();
            products = products.stream()
                .filter(p -> p.getGender() == criteria.getGender())
                .collect(Collectors.toList());
            System.out.println("║ ✂️ After gender filter: " + products.size() + " (removed: " + (beforeFilter - products.size()) + ")");
        }

        if (criteria.getVolume() != null) {
            int beforeFilter = products.size();
            products = products.stream()
                .filter(p -> p.getVolume() == criteria.getVolume())
                .collect(Collectors.toList());
            System.out.println("║ ✂️ After volume filter: " + products.size() + " (removed: " + (beforeFilter - products.size()) + ")");
        }

        if (criteria.getMinPrice() != null) {
            int beforeFilter = products.size();
            products = products.stream()
                .filter(p -> p.getPrice() >= criteria.getMinPrice())
                .collect(Collectors.toList());
            System.out.println("║ ✂️ After minPrice filter (>=" + criteria.getMinPrice() + "): " + products.size() + " (removed: " + (beforeFilter - products.size()) + ")");
        }

        if (criteria.getMaxPrice() != null) {
            int beforeFilter = products.size();
            products = products.stream()
                .filter(p -> p.getPrice() <= criteria.getMaxPrice())
                .collect(Collectors.toList());
            System.out.println("║ ✂️ After maxPrice filter (<=" + criteria.getMaxPrice() + "): " + products.size() + " (removed: " + (beforeFilter - products.size()) + ")");
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
        System.out.println("║ 🔄 After sorting by [" + criteria.getSortBy() + "]: " + products.size() + " products");

        // Apply limit
        if (criteria.getLimit() != null && criteria.getLimit() > 0) {
            products = products.stream()
                .limit(criteria.getLimit())
                .collect(Collectors.toList());
            System.out.println("║ ✂️ After limit (" + criteria.getLimit() + "): " + products.size() + " products");
        }

        System.out.println("╠════════════════════════════════════════════════════════════════");
        System.out.println("║ 🎯 FINAL RESULT: " + products.size() + " products");
        if (!products.isEmpty()) {
            System.out.println("║ 📦 Products returned:");
            products.stream().limit(5).forEach(p ->
                System.out.println("║    • " + p.getName() + " (Price: " + p.getPrice() + ", Brand: " +
                    (p.getCategory() != null ? p.getCategory().getName() : "N/A") + ")")
            );
        } else {
            System.out.println("║ ⚠️ NO PRODUCTS MATCH THE CRITERIA!");
        }
        System.out.println("╚════════════════════════════════════════════════════════════════");

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
     * Build filter criteria from user intents
     */
    public FilterCriteria buildCriteriaFromIntents(Map<String, Object> intents) {
        FilterCriteria criteria = new FilterCriteria();

        // Set product name (specific search by name)
        if (intents.containsKey("productName")) {
            criteria.setProductName((String) intents.get("productName"));
        }

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

        // Set volume
        if (intents.containsKey("volume")) {
            try {
                criteria.setVolume(Volume.valueOf((String) intents.get("volume")));
            } catch (Exception e) {
                // Ignore
            }
        }

        // Default: only in-stock products
        criteria.setInStockOnly(true);

        // Default: top 10 results
        criteria.setLimit(10);

        // ========== SORTING BASED ON INTENT (WITH PRIORITY) ==========
        // Lower priority queries first, higher priority last (will override)
        String sortBy = "rating"; // Default sort

        // Low priority - Recommendation (general)
        if ((Boolean) intents.getOrDefault("isRecommendation", false)) {
            sortBy = "popular";
        }

        // Medium priority - Price query (general)
        if ((Boolean) intents.getOrDefault("isPriceQuery", false)) {
            sortBy = "price_asc";
        }

        // High priority - Specific queries
        if ((Boolean) intents.getOrDefault("isHotTrend", false)) {
            criteria.setHotTrendOnly(true);
            sortBy = "popular";
        }

        if ((Boolean) intents.getOrDefault("isNewProducts", false)) {
            sortBy = "newest";
            criteria.setLimit(5);
        }

        if ((Boolean) intents.getOrDefault("isTopRated", false)) {
            sortBy = "rating";
            criteria.setMinRating(4.0);
            criteria.setLimit(5);
        }

        if ((Boolean) intents.getOrDefault("isBestSelling", false)) {
            sortBy = "popular"; // Use popular as proxy for best-selling
            criteria.setLimit(5);
        }

        // Highest priority - Price-specific queries
        if ((Boolean) intents.getOrDefault("isCheapQuery", false)) {
            sortBy = "price_asc"; // Sort by price ascending (cheapest first)
            criteria.setLimit(3); // Only show 3 cheapest products
            System.out.println("💰 Cheap query detected → sortBy=price_asc, limit=3");
        }

        if ((Boolean) intents.getOrDefault("isExpensiveQuery", false)) {
            sortBy = "price_desc";
            criteria.setLimit(5);
        }

        criteria.setSortBy(sortBy);

        return criteria;
    }

    /**
     * Calculate similarity between two strings using Levenshtein distance
     * Returns value between 0.0 (no match) and 1.0 (exact match)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;

        return 1.0 - ((double) dp[s1.length()][s2.length()] / maxLen);
    }

    /**
     * Inner class for filter criteria
     */
    public static class FilterCriteria {
        private String productName;  // Specific product name search
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
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

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


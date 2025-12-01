package org.fit.shopnuochoa.service;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.model.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service
 * Tạo context từ database sản phẩm để tăng cường cho LLM
 */
@Service
@RequiredArgsConstructor
public class ProductVectorService {

    private final ProductService productService;

    /**
     * Tạo product embeddings/context cho RAG
     * Đây là simplified version, có thể mở rộng với vector embeddings thực sự
     */
    public String generateProductContext(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "Không tìm thấy sản phẩm phù hợp trong kho.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Danh sách sản phẩm có sẵn:\n\n");

        for (int i = 0; i < Math.min(products.size(), 10); i++) { // Top 10
            Product p = products.get(i);
            context.append(String.format("%d. %s\n", i + 1, p.getName()));
            context.append(String.format("   - Giá: %,.0f VNĐ\n", p.getPrice()));
            context.append(String.format("   - NSX: %s\n", p.getCategory().getName()));
            context.append(String.format("   - Dung tích: %s\n",
                p.getVolume() != null ? p.getVolume().name().replace("ML_", "") + "ml" : "N/A"));
            context.append(String.format("   - Giới tính: %s\n",
                p.getGender() != null ? p.getGender().name() : "N/A"));
            context.append(String.format("   - Đánh giá: %.1f/5 ⭐ (%d lượt)\n",
                p.getAverageRating() != null ? p.getAverageRating() : 0.0,
                p.getRatingCount() != null ? p.getRatingCount() : 0));
            context.append(String.format("   - Còn hàng: %s\n",
                p.isInStock() ? "✓ Còn " + p.getQuantity() + " sản phẩm" : "✗ Hết hàng"));

            if (p.getHotTrend() != null && p.getHotTrend()) {
                context.append("   - 🔥 SẢN PHẨM HOT TREND\n");
            }

            context.append("\n");
        }

        return context.toString();
    }

    /**
     * Tạo summary statistics cho context
     */
    public String generateStatisticsContext(List<Product> allProducts) {
        if (allProducts.isEmpty()) {
            return "";
        }

        Map<String, Long> categoryCount = allProducts.stream()
            .collect(Collectors.groupingBy(p -> p.getCategory().getName(), Collectors.counting()));

        double avgPrice = allProducts.stream()
            .mapToDouble(Product::getPrice)
            .average()
            .orElse(0.0);

        double minPrice = allProducts.stream()
            .mapToDouble(Product::getPrice)
            .min()
            .orElse(0.0);

        double maxPrice = allProducts.stream()
            .mapToDouble(Product::getPrice)
            .max()
            .orElse(0.0);

        StringBuilder stats = new StringBuilder();
        stats.append("Thống kê tổng quan:\n");
        stats.append(String.format("- Tổng số sản phẩm: %d\n", allProducts.size()));
        stats.append(String.format("- Giá trung bình: %,.0f VNĐ\n", avgPrice));
        stats.append(String.format("- Khoảng giá: %,.0f - %,.0f VNĐ\n", minPrice, maxPrice));
        stats.append("\nPhân bố theo nhà sản xuất:\n");
        categoryCount.forEach((cat, count) ->
            stats.append(String.format("  • %s: %d sản phẩm\n", cat, count)));

        return stats.toString();
    }

    /**
     * Tìm sản phẩm tương tự (simple similarity)
     * Có thể mở rộng với vector similarity search
     */
    public List<Product> findSimilarProducts(Product product, List<Product> allProducts, int limit) {
        return allProducts.stream()
            .filter(p -> !p.getId().equals(product.getId()))
            .filter(p -> p.getCategory().getId().equals(product.getCategory().getId()) ||
                        p.getGender() == product.getGender() ||
                        Math.abs(p.getPrice() - product.getPrice()) < 500000)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Extract keywords từ câu hỏi (Simple NLP)
     */
    public Map<String, Object> extractQueryIntents(String query) {
        Map<String, Object> intents = new HashMap<>();
        String queryLower = query.toLowerCase();

        // Intent detection
        intents.put("isPriceQuery", queryLower.matches(".*(giá|bao nhiêu|tiền|rẻ|đắt).*"));
        intents.put("isRecommendation", queryLower.matches(".*(gợi ý|tư vấn|nên|mua|chọn|tốt).*"));
        intents.put("isComparison", queryLower.matches(".*(so sánh|khác|giống|hơn).*"));
        intents.put("isAvailability", queryLower.matches(".*(còn|hết|tồn kho|có sẵn).*"));

        // Gender extraction
        if (queryLower.contains("nam")) intents.put("gender", "NAM");
        else if (queryLower.contains("nữ") || queryLower.contains("phụ nữ")) intents.put("gender", "NU");
        else if (queryLower.contains("unisex")) intents.put("gender", "UNISEX");

        // Price range extraction
        if (queryLower.contains("dưới") && queryLower.matches(".*\\d+.*")) {
            intents.put("maxPrice", extractNumber(query));
        }
        if (queryLower.contains("trên") && queryLower.matches(".*\\d+.*")) {
            intents.put("minPrice", extractNumber(query));
        }

        // Brand extraction (simple)
        List<String> brands = List.of("dior", "chanel", "gucci", "versace", "prada");
        for (String brand : brands) {
            if (queryLower.contains(brand)) {
                intents.put("brand", brand);
                break;
            }
        }

        return intents;
    }

    private Double extractNumber(String text) {
        try {
            String numbers = text.replaceAll("[^0-9]", "");
            if (!numbers.isEmpty()) {
                return Double.parseDouble(numbers);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}


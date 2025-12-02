package org.fit.shopnuochoa.service;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Product;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG (Retrieval-Augmented Generation) Service
 * Tạo context từ database sản phẩm để tăng cường cho LLM
 */
@Service
@RequiredArgsConstructor
public class ProductVectorService {

    private final CategoryService categoryService;
    private final ProductStatisticsService statisticsService;

    /**
     * Tạo context đặc biệt cho sản phẩm bán chạy
     * Format rõ ràng để LLM thấy số lượng bán thực tế
     */
    public String generateBestSellingContext(List<ProductStatisticsService.ProductStats> bestSellers) {
        if (bestSellers == null || bestSellers.isEmpty()) {
            return "CẢNH BÁO: Không có dữ liệu bán hàng.\n" +
                   "HƯỚNG DẪN: Thông báo cho khách hàng chưa có sản phẩm nào được bán.";
        }

        StringBuilder context = new StringBuilder();
        context.append("===== TOP SẢN PHẨM BÁN CHẠY NHẤT =====\n");
        context.append("DỮ LIỆU THỰC TẾ từ bảng OrderLines, được sắp xếp theo số lượng bán giảm dần:\n\n");

        for (int i = 0; i < bestSellers.size(); i++) {
            ProductStatisticsService.ProductStats ps = bestSellers.get(i);
            Product p = ps.getProduct();

            context.append(String.format("【XẾP HẠNG #%d - BÁN CHẠY NHẤT】\n", i + 1));
            context.append(String.format("├─ Tên: %s\n", p.getName()));
            context.append(String.format("├─ ID: %d\n", p.getId()));
            context.append(String.format("├─ Giá: %,.0f VNĐ\n", p.getPrice()));
            context.append(String.format("├─ Thương hiệu: %s\n", p.getCategory().getName()));
            context.append(String.format("├─ Dung tích: %s\n",
                p.getVolume() != null ? p.getVolume().name().replace("ML_", "") + "ml" : "Không rõ"));
            context.append(String.format("├─ Giới tính: %s\n",
                p.getGender() != null ? p.getGender().name() : "Không rõ"));

            // QUAN TRỌNG: Số lượng đã bán THỰC TẾ
            context.append(String.format("├─ ⭐ ĐÃ BÁN: %d sản phẩm (Dữ liệu chính xác từ OrderLines)\n",
                ps.getTotalSold()));
            context.append(String.format("├─ Doanh thu: %,.0f VNĐ\n", ps.getTotalRevenue()));

            // Đánh giá
            if (p.getAverageRating() != null && p.getAverageRating() > 0) {
                context.append(String.format("├─ Đánh giá: %.1f/5 ⭐ (%d lượt)\n",
                    p.getAverageRating(),
                    p.getRatingCount() != null ? p.getRatingCount() : 0));
            } else {
                context.append("├─ Đánh giá: Chưa có\n");
            }

            // Tồn kho
            if (p.isInStock()) {
                context.append(String.format("└─ Tồn kho: CÒN HÀNG (%d sản phẩm)\n", p.getQuantity()));
            } else {
                context.append("└─ Tồn kho: HẾT HÀNG\n");
            }

            context.append("\n");
        }

        context.append("===== KẾT THÚC BẢNG XẾP HẠNG =====\n");
        context.append("CHÚ THÍCH: Số liệu \"ĐÃ BÁN\" là tổng số lượng từ TẤT CẢ đơn hàng thành công.\n");
        context.append("CẢNH BÁO: CHỈ sử dụng số liệu \"ĐÃ BÁN\" được ghi rõ ở trên, KHÔNG được đoán mò.\n");

        return context.toString();
    }

    /**
     * Tạo product embeddings/context cho RAG với thông tin bán hàng thực tế
     * Context này được format để LLM không thể bịa thông tin
     */
    public String generateProductContext(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "CẢNH BÁO: Không tìm thấy sản phẩm phù hợp trong kho.\n" +
                   "HƯỚNG DẪN: Thông báo cho khách hàng rằng hiện không có sản phẩm phù hợp.";
        }

        StringBuilder context = new StringBuilder();
        context.append("===== DỮ LIỆU SẢN PHẨM THỰC TẾ =====\n");
        context.append("CẢNH BÁO: CHỈ sử dụng thông tin dưới đây. KHÔNG bịa số liệu.\n\n");

        for (int i = 0; i < Math.min(products.size(), 10); i++) {
            Product p = products.get(i);

            // Lấy số lượng đã bán THỰC TẾ
            Integer totalSold = statisticsService.getTotalSoldByProductId(p.getId());

            context.append(String.format("【SẢN PHẨM #%d】\n", i + 1));
            context.append(String.format("├─ Tên: %s\n", p.getName()));
            context.append(String.format("├─ ID: %d\n", p.getId()));
            context.append(String.format("├─ Giá: %,.0f VNĐ (Chính xác)\n", p.getPrice()));
            context.append(String.format("├─ Thương hiệu: %s\n", p.getCategory().getName()));
            context.append(String.format("├─ Dung tích: %s\n",
                p.getVolume() != null ? p.getVolume().name().replace("ML_", "") + "ml" : "Không rõ"));
            context.append(String.format("├─ Giới tính: %s\n",
                p.getGender() != null ? p.getGender().name() : "Không rõ"));

            // Thông tin đánh giá - RÕ RÀNG
            if (p.getAverageRating() != null && p.getAverageRating() > 0) {
                context.append(String.format("├─ Đánh giá: %.1f/5 ⭐ (%d lượt đánh giá)\n",
                    p.getAverageRating(),
                    p.getRatingCount() != null ? p.getRatingCount() : 0));
            } else {
                context.append("├─ Đánh giá: Chưa có đánh giá\n");
            }

            // Thông tin tồn kho - RÕ RÀNG
            if (p.isInStock()) {
                context.append(String.format("├─ Tồn kho: CÒN HÀNG (%d sản phẩm)\n", p.getQuantity()));
            } else {
                context.append("├─ Tồn kho: HẾT HÀNG\n");
            }

            // Thông tin bán hàng - RÕ RÀNG VÀ THỰC TẾ
            if (totalSold != null && totalSold > 0) {
                context.append(String.format("├─ Đã bán: %d sản phẩm (Dữ liệu thực tế từ OrderLines)\n", totalSold));
            } else {
                context.append("├─ Đã bán: 0 sản phẩm (Chưa có đơn hàng nào)\n");
            }

            // Hot trend
            if (p.getHotTrend() != null && p.getHotTrend()) {
                context.append("└─ Đặc biệt: 🔥 HOT TREND\n");
            } else {
                context.append("└─ Đặc biệt: Không\n");
            }

            context.append("\n");
        }

        context.append("===== KẾT THÚC DỮ LIỆU =====\n");
        context.append("LƯU Ý: Tất cả thông tin trên là CHÍNH XÁC từ database.\n");
        context.append("KHÔNG được tự ý thêm/sửa/bịa số liệu không có trong danh sách.\n");

        return context.toString();
    }


    /**
     * Extract keywords từ câu hỏi (Enhanced NLP với nhiều intent hơn)
     */
    public Map<String, Object> extractQueryIntents(String query) {
        Map<String, Object> intents = new HashMap<>();
        String queryLower = query.toLowerCase();

        // ========== INTENT DETECTION ==========
        // 1. Giá cả
        intents.put("isPriceQuery",
            queryLower.matches(".*(giá|bao nhiêu|tiền|chi phí|giá bán).*"));
        intents.put("isCheapQuery",
            queryLower.matches(".*(rẻ|giá tốt|phải chăng|tiết kiệm|giảm giá|khuyến mãi).*"));
        intents.put("isExpensiveQuery",
            queryLower.matches(".*(đắt|cao cấp|sang trọng|xa xỉ|mắc|đắt nhất).*"));

        // 2. Đề xuất/Tư vấn
        intents.put("isRecommendation",
            queryLower.matches(".*(gợi ý|tư vấn|nên|mua|chọn|đề xuất|giới thiệu).*"));

        // 3. So sánh
        intents.put("isComparison",
            queryLower.matches(".*(so sánh|khác|giống|hơn|tốt hơn).*"));

        // 4. Tồn kho
        intents.put("isAvailability",
            queryLower.matches(".*(còn|hết|tồn kho|có sẵn|có bán).*"));

        // 5. Bán chạy
        intents.put("isBestSelling",
            queryLower.matches(".*(bán chạy|phổ biến|nổi tiếng|hot|trend|xu hướng|nhiều người mua).*"));

        // 6. Đánh giá cao
        intents.put("isTopRated",
            queryLower.matches(".*(đánh giá cao|rating cao|review tốt|tốt nhất|chất lượng).*"));

        // 7. Mới
        intents.put("isNewProducts",
            queryLower.matches(".*(mới|ra mắt|mới nhất|latest|new).*"));

        // 8. Hot Trend
        intents.put("isHotTrend",
            queryLower.matches(".*(hot trend|hot|trend|thịnh hành|đang được ưa chuộng).*"));

        // ========== GENDER EXTRACTION ==========
        if (queryLower.matches(".*(nam|đàn ông|for men|men).*") &&
            !queryLower.contains("nữ") && !queryLower.contains("phụ nữ")) {
            intents.put("gender", "NAM");
        } else if (queryLower.matches(".*(nữ|phụ nữ|for women|women|cô gái).*")) {
            intents.put("gender", "NU");
        } else if (queryLower.matches(".*(unisex|cả nam và nữ|nam nữ).*")) {
            intents.put("gender", "UNISEX");
        }

        // ========== PRICE RANGE EXTRACTION ==========
        // Dưới X triệu/nghìn
        if (queryLower.matches(".*(dưới|nhỏ hơn|ít hơn|không quá).*\\d+.*(triệu|nghìn|k|tr).*")) {
            Double maxPrice = extractPriceFromQuery(query);
            if (maxPrice != null) intents.put("maxPrice", maxPrice);
        }

        // Trên X triệu/nghìn
        if (queryLower.matches(".*(trên|lớn hơn|nhiều hơn|từ).*\\d+.*(triệu|nghìn|k|tr).*")) {
            Double minPrice = extractPriceFromQuery(query);
            if (minPrice != null) intents.put("minPrice", minPrice);
        }

        // Khoảng X - Y triệu
        if (queryLower.matches(".*\\d+.*(đến|-).*\\d+.*(triệu|nghìn|k|tr).*")) {
            Map<String, Double> range = extractPriceRange(query);
            if (range != null) {
                intents.put("minPrice", range.get("min"));
                intents.put("maxPrice", range.get("max"));
            }
        }

        // ========== BRAND EXTRACTION ==========
        List<Category> brands = categoryService.getAll().stream().toList();
        for (Category brand : brands) {
            if (queryLower.contains(brand.getName())) {
                intents.put("brand", brand);
                break;
            }
        }

        // ========== VOLUME EXTRACTION ==========
        if (queryLower.matches(".*(30ml|30 ml).*")) intents.put("volume", "ML_30");
        else if (queryLower.matches(".*(50ml|50 ml).*")) intents.put("volume", "ML_50");
        else if (queryLower.matches(".*(75ml|75 ml).*")) intents.put("volume", "ML_75");
        else if (queryLower.matches(".*(100ml|100 ml).*")) intents.put("volume", "ML_100");

        return intents;
    }

    /**
     * Extract giá từ query với đơn vị (triệu, nghìn, k)
     */
    private Double extractPriceFromQuery(String text) {
        try {
            String lower = text.toLowerCase();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(triệu|nghìn|k|tr)");
            java.util.regex.Matcher matcher = pattern.matcher(lower);

            if (matcher.find()) {
                double number = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2);

                // Chuyển đổi về VNĐ
                if (unit.equals("triệu") || unit.equals("tr")) {
                    return number * 1_000_000;
                } else if (unit.equals("nghìn") || unit.equals("k")) {
                    return number * 1_000;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Extract khoảng giá từ query (VD: 1-2 triệu)
     */
    private Map<String, Double> extractPriceRange(String text) {
        try {
            String lower = text.toLowerCase();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:đến|-)\\s*(\\d+)\\s*(triệu|nghìn|k|tr)");
            java.util.regex.Matcher matcher = pattern.matcher(lower);

            if (matcher.find()) {
                double num1 = Double.parseDouble(matcher.group(1));
                double num2 = Double.parseDouble(matcher.group(2));
                String unit = matcher.group(3);

                double multiplier = 1;
                if (unit.equals("triệu") || unit.equals("tr")) {
                    multiplier = 1_000_000;
                } else if (unit.equals("nghìn") || unit.equals("k")) {
                    multiplier = 1_000;
                }

                Map<String, Double> range = new HashMap<>();
                range.put("min", num1 * multiplier);
                range.put("max", num2 * multiplier);
                return range;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}


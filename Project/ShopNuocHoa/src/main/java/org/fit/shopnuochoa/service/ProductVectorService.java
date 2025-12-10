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


    private final org.springframework.ai.chat.client.ChatClient.Builder chatClientBuilder;

    /**
     * Extract keywords từ câu hỏi bằng LLM (AI-powered extraction)
     * LLM sẽ phân tích câu hỏi và trích xuất các tiêu chí lọc
     */
    public Map<String, Object> extractQueryIntents(String query) {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🤖 LLM-BASED CRITERIA EXTRACTION");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📝 Query: [" + query + "]");

        Map<String, Object> intents = new HashMap<>();
        String queryLower = query.toLowerCase();

        // ========== STEP 1: Use LLM to extract criteria ==========
        Map<String, Object> llmCriteria = extractCriteriaUsingLLM(query);
        intents.putAll(llmCriteria);

        // ========== STEP 2: Fallback - Use regex for critical fields if LLM fails ==========
        // Simple greeting detection (always use regex for performance)
        if (!intents.containsKey("isGreeting")) {
            intents.put("isGreeting",
                queryLower.matches("^(xin chào|chào|hello|hi|hey|chào bạn|chào shop|chào cửa hàng)$|^(xin chào|chào|hello|hi|hey)\\s*[!.?]*$"));
        }

        // ========== STEP 3: Extract brand from database ==========
        if (!intents.containsKey("brand") || intents.get("brand") == null) {
            String brand = extractBrandFromQuery(queryLower);
            if (brand != null) {
                intents.put("brand", brand);
            }
        }

        // ========== FINAL DEBUG LOGGING ==========
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔍 INTENT EXTRACTION DEBUG");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📝 Query: [" + query + "]");
        System.out.println("📝 Query (lowercase): [" + queryLower + "]");
        System.out.println("-------------------------------------------");
        System.out.println("🎯 Extracted Intents:");
        System.out.println("  • isExpensiveQuery: " + intents.get("isExpensiveQuery"));
        System.out.println("  • isCheapQuery: " + intents.get("isCheapQuery"));
        System.out.println("  • isBestSelling: " + intents.get("isBestSelling"));
        System.out.println("  • isHotTrend: " + intents.get("isHotTrend"));
        System.out.println("-------------------------------------------");
        System.out.println("💰 Price Filters:");
        System.out.println("  • minPrice: " + intents.get("minPrice"));
        System.out.println("  • maxPrice: " + intents.get("maxPrice"));
        System.out.println("-------------------------------------------");
        System.out.println("🏷️ Other Filters:");
        System.out.println("  • brand: " + intents.get("brand"));
        System.out.println("  • gender: " + intents.get("gender"));
        System.out.println("  • volume: " + intents.get("volume"));
        System.out.println("  • productName: " + intents.get("productName"));
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return intents;
    }

    /**
     * Use LLM (Gemini) to extract criteria from user query
     * Returns structured JSON with filter criteria
     */
    private Map<String, Object> extractCriteriaUsingLLM(String query) {
        try {
            String systemPrompt = """
                Bạn là AI phân tích câu hỏi về nước hoa và trích xuất tiêu chí lọc sản phẩm.
                
                NHIỆM VỤ: Phân tích câu hỏi và trả về JSON với các tiêu chí sau:
                
                1. INTENTS (Boolean):
                   - isGreeting: câu chào (hi, hello, chào)
                   - isPriceQuery: hỏi về giá
                   - isCheapQuery: tìm sản phẩm rẻ/giá tốt/sinh viên/học sinh/bình dân/giá mềm
                     → Khi detect, hệ thống sẽ TỰ ĐỘNG sort giá tăng dần và chỉ lấy 3 sản phẩm rẻ nhất
                   - isExpensiveQuery: tìm sản phẩm đắt/mắc/cao cấp/sang trọng
                   - isRecommendation: xin gợi ý/tư vấn
                   - isBestSelling: tìm sản phẩm bán chạy/phổ biến
                   - isHotTrend: tìm sản phẩm hot trend/thịnh hành
                   - isNewProducts: tìm sản phẩm mới/mới nhất
                   - isTopRated: tìm sản phẩm đánh giá cao
                
                2. PRICE FILTERS (Double, đơn vị VNĐ):
                   - minPrice: giá tối thiểu (ví dụ: "trên 1 triệu" → 1000000)
                   - maxPrice: giá tối đa (ví dụ: "dưới 500k" → 500000)
                   
                   ⚠️ LƯU Ý: Với isCheapQuery, CHỈ set nếu có giá CỤ THỂ
                   - "giá sinh viên" → isCheapQuery=true (KHÔNG set maxPrice)
                   - "giá rẻ" → isCheapQuery=true (KHÔNG set maxPrice)
                   - "dưới 500k" → isCheapQuery=true, maxPrice=500000
                   
                3. OTHER FILTERS (String):
                   - gender: NAM/NU/UNISEX (nếu có từ nam/nữ/unisex)
                   - productName: tên sản phẩm cụ thể (nếu hỏi về sản phẩm cụ thể)
                   - brandKeyword: từ khóa thương hiệu (dior, chanel, gucci...)
                
                QUY TẮC CHUYỂN ĐỔI GIÁ:
                - "k" hoặc "nghìn" hoặc "ngàn" → nhân 1000
                - "triệu" hoặc "tr" → nhân 1000000
                - "500k" → 500000
                - "3 triệu" → 3000000
                - "500.000" hoặc "500,000" → 500000 (bỏ dấu chấm/phẩy)
                
                FORMAT TRẢ VỀ: JSON thuần túy, KHÔNG có markdown, KHÔNG có ```json
                
                VÍ DỤ:
                Query: "sản phẩm giá sinh viên"
                Response: {"isCheapQuery":true,"isPriceQuery":true}
                
                Query: "sản phẩm giá rẻ"
                Response: {"isCheapQuery":true,"isPriceQuery":true}
                
                Query: "sản phẩm dưới 500k"
                Response: {"isCheapQuery":true,"isPriceQuery":true,"maxPrice":500000}
                
                Query: "nước hoa nam giá từ 2 đến 3 triệu"
                Response: {"gender":"NAM","isPriceQuery":true,"minPrice":2000000,"maxPrice":3000000}
                
                Query: "sản phẩm đắt nhất của Dior"
                Response: {"isExpensiveQuery":true,"brandKeyword":"dior"}
                """;

            String userPrompt = "Câu hỏi: " + query;

            org.springframework.ai.chat.client.ChatClient chatClient = chatClientBuilder.build();
            String llmResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

            System.out.println("🤖 LLM Raw Response: " + llmResponse);

            // Parse JSON response
            llmResponse = llmResponse.trim();
            // Remove markdown code blocks if present
            if (llmResponse.startsWith("```")) {
                llmResponse = llmResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }

            Map<String, Object> criteria = parseJsonResponse(llmResponse);
            System.out.println("✅ LLM Extracted Criteria: " + criteria);

            return criteria;

        } catch (Exception e) {
            System.out.println("⚠️ LLM extraction failed: " + e.getMessage());
            System.out.println("   Falling back to regex-based extraction...");
            return new HashMap<>(); // Return empty, will use regex fallback
        }
    }

    /**
     * Simple JSON parser for LLM response
     */
    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Remove outer braces
            json = json.trim();
            if (json.startsWith("{")) json = json.substring(1);
            if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

            // Split by comma (basic parsing)
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");

                    // Parse value type
                    if (value.equalsIgnoreCase("true")) {
                        result.put(key, true);
                    } else if (value.equalsIgnoreCase("false")) {
                        result.put(key, false);
                    } else if (value.matches("\\d+(\\.\\d+)?")) {
                        result.put(key, Double.parseDouble(value));
                    } else {
                        result.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ JSON parsing error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Extract brand from query by matching with database brands
     */
    private String extractBrandFromQuery(String queryLower) {
        List<Category> brands = categoryService.getAll().stream().toList();
        for (Category brand : brands) {
            String brandNameLower = brand.getName().toLowerCase().trim();
            if (queryLower.contains(brandNameLower)) {
                System.out.println("✅ Brand matched: " + brand.getName());
                return brand.getName();
            }
        }
        return null;
    }

    /**
     * Extract product name from user query (kept as fallback)
     */
    private String extractProductName(String query) {
        String cleaned = query.toLowerCase();

        // Check if this is NOT a product name query but a criteria query
        // These patterns indicate criteria search, not product name search
        String[] criteriaPatterns = {
            ".*phù hợp.*", ".*thích hợp.*", ".*dành cho.*", ".*cho.*nam.*", ".*cho.*nữ.*",
            ".*nam giới.*", ".*nữ giới.*", ".*đàn ông.*", ".*phụ nữ.*",
            ".*bán chạy.*", ".*hot.*trend.*", ".*đánh giá cao.*", ".*mới nhất.*",
            ".*giá rẻ.*", ".*giá cao.*", ".*rẻ nhất.*", ".*đắt nhất.*"
        };

        for (String pattern : criteriaPatterns) {
            if (cleaned.matches(pattern)) {
                return null; // This is criteria search, not product name
            }
        }

        // Remove common question words and keywords
        String[] stopWords = {
            "có", "không", "nào", "giá", "bao nhiêu", "tiền", "chi phí",
            "tìm", "kiếm", "cho", "tôi", "mua", "bán", "của", "nhà", "hãng",
            "sản phẩm", "loại", "nước hoa", "còn", "hết", "có bán", "giới thiệu",
            "gợi ý", "tư vấn", "đề xuất", "có sẵn", "phù hợp", "thích hợp", "dành"
        };

        for (String stopWord : stopWords) {
            cleaned = cleaned.replaceAll("\\b" + stopWord + "\\b", " ");
        }

        // Remove price patterns
        cleaned = cleaned.replaceAll("\\d+\\s*(triệu|nghìn|k|tr|vnđ|đồng|vnd)", " ");

        // Remove special characters but keep alphanumeric and Vietnamese
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\sàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]", " ");

        // Clean up multiple spaces
        cleaned = cleaned.trim().replaceAll("\\s+", " ");

        // If result is too short or empty, return null
        if (cleaned.length() < 3) {
            return null;
        }

        // Final check: if cleaned result contains generic words only, return null
        String[] genericWords = {"nam", "nữ", "unisex", "giới", "tính"};
        boolean onlyGeneric = true;
        String[] words = cleaned.split("\\s+");
        for (String word : words) {
            boolean isGeneric = false;
            for (String generic : genericWords) {
                if (word.equals(generic)) {
                    isGeneric = true;
                    break;
                }
            }
            if (!isGeneric) {
                onlyGeneric = false;
                break;
            }
        }

        if (onlyGeneric) {
            return null;
        }

        return cleaned;
    }

    /**
     * Extract giá từ query với đơn vị (triệu, nghìn, k)
     * Hỗ trợ format: 500k, 500 k, 500nghìn, 500.000, 500,000
     */
    private Double extractPriceFromQuery(String text) {
        try {
            String lower = text.toLowerCase();
            // Pattern hỗ trợ số có dấu chấm/phẩy: 500.000, 500,000, hoặc số thường: 500
            // Tìm pattern: số + đơn vị (triệu/nghìn/k/tr)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d+(?:[.,]\\d+)*)\\s*(triệu|nghìn|ngàn|k|tr)");
            java.util.regex.Matcher matcher = pattern.matcher(lower);

            Double result = null;
            while (matcher.find()) {
                String numberStr = matcher.group(1).replaceAll("[.,]", ""); // Remove dots/commas
                String unit = matcher.group(2);

                double number = Double.parseDouble(numberStr);

                // Convert based on unit
                if (unit.equals("triệu") || unit.equals("tr")) {
                    result = number * 1_000_000;
                } else if (unit.equals("nghìn") || unit.equals("ngàn") || unit.equals("k")) {
                    result = number * 1_000;
                }

                // Return first valid price found
                if (result != null) {
                    System.out.println("✅ Extracted price: " + result + " VNĐ from [" + matcher.group(0) + "]");
                    return result;
                }
            }

            if (result == null) {
                System.out.println("⚠️ No price found in: [" + text + "]");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error extracting price from: " + text + " - " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract khoảng giá từ query (VD: 1-2 triệu, 500k-1tr, 2 đến 3 triệu)
     * Hỗ trợ format với dấu chấm/phẩy
     */
    private Map<String, Double> extractPriceRange(String text) {
        try {
            String lower = text.toLowerCase();
            // Pattern hỗ trợ: "2-3 triệu", "2 đến 3 triệu", "500.000-1.000.000"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d+(?:[.,]\\d+)*)\\s*(?:đến|-)\\s*(\\d+(?:[.,]\\d+)*)\\s*(triệu|nghìn|ngàn|k|tr)");
            java.util.regex.Matcher matcher = pattern.matcher(lower);

            if (matcher.find()) {
                String num1Str = matcher.group(1).replaceAll("[.,]", "");
                String num2Str = matcher.group(2).replaceAll("[.,]", "");
                String unit = matcher.group(3);

                double num1 = Double.parseDouble(num1Str);
                double num2 = Double.parseDouble(num2Str);

                double multiplier = 1;
                if (unit.equals("triệu") || unit.equals("tr")) {
                    multiplier = 1_000_000;
                } else if (unit.equals("nghìn") || unit.equals("ngàn") || unit.equals("k")) {
                    multiplier = 1_000;
                }

                Map<String, Double> range = new HashMap<>();
                range.put("min", num1 * multiplier);
                range.put("max", num2 * multiplier);

                System.out.println("✅ Extracted price range: " + range.get("min") + " - " + range.get("max"));
                return range;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error extracting price range from: " + text + " - " + e.getMessage());
        }
        return null;
    }
}


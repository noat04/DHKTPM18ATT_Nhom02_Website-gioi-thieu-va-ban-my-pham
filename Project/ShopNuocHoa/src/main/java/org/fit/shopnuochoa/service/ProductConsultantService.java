package org.fit.shopnuochoa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fit.shopnuochoa.model.Product;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid LLM + Structured Filter + RAG Product Consultant Service
 *
 * Architecture:
 * 1. Intent Extraction: Phân tích câu hỏi người dùng
 * 2. Structured Filter: Lọc sản phẩm theo tiêu chí
 * 3. RAG: Tạo context từ database
 * 4. LLM Generation: Tạo câu trả lời tự nhiên
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductConsultantService {

    private final ChatClient.Builder chatClientBuilder;
    private final ProductVectorService vectorService;
    private final ProductFilterEngine filterEngine;
    private final ProductStatisticsService statisticsService;

    /**
     * Consultation result wrapper
     */
    public static class ConsultationResult {
        private final String response;
        private final List<Product> products;

        public ConsultationResult(String response, List<Product> products) {
            this.response = response;
            this.products = products;
        }

        public String getResponse() { return response; }
        public List<Product> getProducts() { return products; }
    }

    /**
     * Main consultation method with products - Returns both response and product list
     */
    public ConsultationResult consultProductWithDetails(String userQuery) {
        try {
            log.info("🔍 Starting Hybrid Consultation for query: {}", userQuery);

            // ========== PHASE 1: INTENT EXTRACTION ==========
            Map<String, Object> intents = vectorService.extractQueryIntents(userQuery);
            log.info("📊 Extracted intents: {}", intents);

            // ========== GREETING HANDLING ==========
            if ((Boolean) intents.getOrDefault("isGreeting", false)) {
                log.info("👋 Greeting detected");
                String greetingResponse = "Chào bạn! Tôi là trợ lý AI tư vấn nước hoa của ShopNuocHoa. Bạn có cần tôi giúp đỡ gì không?";
                return new ConsultationResult(greetingResponse, List.of());
            }

            // ========== PHASE 2: STRUCTURED FILTERING ==========
            // Build criteria from all intents without prioritization
            ProductFilterEngine.FilterCriteria criteria = filterEngine.buildCriteriaFromIntents(intents);
            log.info("📋 Filter criteria: minPrice={}, maxPrice={}, brand={}, gender={}, sortBy={}",
                criteria.getMinPrice(), criteria.getMaxPrice(), criteria.getCategoryName(),
                criteria.getGender(), criteria.getSortBy());

            List<Product> filteredProducts = filterEngine.filterProducts(criteria);

            // Generate context based on special queries for better LLM response
            String productContext = vectorService.generateProductContext(filteredProducts);
            String statisticsContext = "";

            // Only add statistics context for best-selling queries to show actual sales numbers
            if ((Boolean) intents.getOrDefault("isBestSelling", false) && !filteredProducts.isEmpty()) {
                log.info("🏆 Including best-selling statistics for reference");
                statisticsContext = statisticsService.generateEnhancedStatistics();
            }

            log.info("🔎 Filtered {} products with criteria: {}", filteredProducts.size(), intents);
            if (filteredProducts.isEmpty()) {
                log.warn("⚠️ No products found for query: {}", userQuery);
            } else {
                log.info("📦 First 3 products: {}", filteredProducts.stream()
                    .limit(3)
                    .map(Product::getName)
                    .collect(java.util.stream.Collectors.joining(", ")));
            }

            log.info("📚 Generated context with {} products", filteredProducts.size());

            // ========== PHASE 4: LLM GENERATION ==========
            String enhancedResponse = generateLLMResponse(
                userQuery,
                productContext,
                statisticsContext,
                intents
            );

            log.info("✅ Consultation completed successfully");

            // Determine how many products to return based on query intent and available products
            int maxProductsToReturn = 3; // Default

            // For superlative queries (nhất), return only 1 product
            String queryLower = userQuery.toLowerCase();
            if (queryLower.matches(".*(đắt nhất|rẻ nhất|mắc nhất|cao nhất|thấp nhất|tốt nhất|bán chạy nhất|phổ biến nhất|nổi tiếng nhất).*")) {
                maxProductsToReturn = 1;
                log.info("🎯 Superlative query detected - returning only 1 product");
            }

            // Return products based on what's available
            List<Product> productsToReturn;
            if (filteredProducts.isEmpty()) {
                productsToReturn = List.of(); // No products found
            } else {
                // Return min(available, maxToReturn)
                int actualLimit = Math.min(filteredProducts.size(), maxProductsToReturn);
                productsToReturn = filteredProducts.stream()
                    .limit(actualLimit)
                    .collect(java.util.stream.Collectors.toList());
            }

            // Log the exact products being returned
            if (!productsToReturn.isEmpty()) {
                log.info("🎯 Returning {} product(s) to client:", productsToReturn.size());
                productsToReturn.forEach(p ->
                    log.info("  → {} (ID: {}, Price: {}, Brand: {})",
                        p.getName(), p.getId(), p.getPrice(),
                        p.getCategory() != null ? p.getCategory().getName() : "N/A")
                );
            } else {
                log.info("🎯 No products to return");
            }

            return new ConsultationResult(enhancedResponse, productsToReturn);

        } catch (Exception e) {
            log.error("❌ Error in hybrid consultation: ", e);
            return new ConsultationResult("Xin lỗi, đã có lỗi xảy ra khi tư vấn. Vui lòng thử lại sau.", List.of());
        }
    }

    /**
     * Generate response using LLM with RAG context
     */
    private String generateLLMResponse(
            String userQuery,
            String productContext,
            String statisticsContext,
            Map<String, Object> intents) {

        // Build enhanced prompt with RAG
        String systemPrompt = """
            Bạn là chuyên gia tư vấn nước hoa chuyên nghiệp tại cửa hàng ShopNuocHoa.
            
            ⚠️ QUY TẮC BẮT BUỘC - KHÔNG ĐƯỢC VI PHẠM:
            
            🚨 QUY TẮC QUAN TRỌNG NHẤT:
            - Phần [DỮ LIỆU SẢN PHẨM THỰC TẾ] bên dưới là KẾT QUẢ LỌC theo yêu cầu khách hàng
            - CHỈ giới thiệu các sản phẩm có trong phần [DỮ LIỆU SẢN PHẨM THỰC TẾ]
            - TUYỆT ĐỐI KHÔNG lấy sản phẩm từ phần [TOP SẢN PHẨM] nếu nó không có trong [DỮ LIỆU SẢN PHẨM THỰC TẾ]
            - Phần [TOP SẢN PHẨM] CHỈ để tham khảo số liệu, KHÔNG phải danh sách sản phẩm cần giới thiệu
            
            1. ❌ TUYỆT ĐỐI KHÔNG bịa đặt, suy đoán, hoặc thêm thông tin không có trong dữ liệu
            2. ✅ CHỈ giới thiệu sản phẩm từ mục [DỮ LIỆU SẢN PHẨM THỰC TẾ]
            3. ✅ Nếu không có thông tin về một trường nào đó, hãy BỎ QUA, ĐỪNG đoán
            4. ✅ Số liệu "Đã bán" CHỈ lấy từ trường "Đã bán" trong dữ liệu
            5. ✅ Giá tiền, rating, tồn kho phải CHÍNH XÁC 100%
            6. ✅ KHI thấy "CẢNH BÁO: Không tìm thấy sản phẩm phù hợp trong kho":
               - Đây có nghĩa là HỆ THỐNG đã lọc và KHÔNG CÓ sản phẩm nào phù hợp
               - Trả lời: "Chào bạn, rất tiếc hiện tại không có sản phẩm [mô tả yêu cầu] trong danh mục của chúng tôi."
            7. ✅ KHI CÓ danh sách trong [DỮ LIỆU SẢN PHẨM THỰC TẾ]:
               - Giới thiệu từ 2-3 sản phẩm đầu tiên trong danh sách ĐÓ
               - KHÔNG lấy sản phẩm từ phần thống kê hoặc bất kỳ nguồn nào khác
            
            CÁCH TRẢ LỜI KHI TÌM THẤY SẢN PHẨM:
            - Nếu câu hỏi về "NHẤT" (đắt nhất, rẻ nhất, bán chạy nhất...): CHỈ giới thiệu 1 sản phẩm duy nhất
            - Nếu câu hỏi tổng quát: Giới thiệu 2-3 sản phẩm
            - Ngắn gọn, liệt kê rõ ràng
            - Mỗi sản phẩm: Tên, Giá, Thương hiệu
            - Nếu có "Đã bán": Nói rõ "Đã bán X sản phẩm"
            - Nếu KHÔNG có "Đã bán" hoặc = 0: ĐỪNG nói về số lượng bán
            - Format với "NHẤT": "Sản phẩm [tiêu chí] nhất là [Tên] ([Giá])"
            - Format tổng quát: "Tôi gợi ý: 1) [SP1], 2) [SP2], 3) [SP3]"
            
            CÁCH TRẢ LỜI KHI KHÔNG TÌM THẤY:
            - Thông báo không tìm thấy sản phẩm cụ thể
            - KHÔNG đề xuất sản phẩm khác trừ khi dữ liệu có sản phẩm tương tự
            
            VÍ DỤ TRẢ LỜI ĐÚNG:
            ✅ "Chào bạn, tôi gợi ý 3 sản phẩm: 1) Dior Sauvage (2,500,000 VNĐ), 2) Chanel Bleu (3,200,000 VNĐ), 3) CK One (450,000 VNĐ)."
            ✅ "Với yêu cầu của bạn, có 2 sản phẩm phù hợp: 1) Gucci Bloom (2,950,000 VNĐ), 2) Chanel N5 (3,500,000 VNĐ)."
            ✅ "Chào bạn, rất tiếc hiện tại không có sản phẩm trong khoảng giá từ 2-3 triệu trong danh mục của chúng tôi."
            ✅ "Chào bạn, sản phẩm đắt nhất của Dior là J'adore Eau de Parfum (3,800,000 VNĐ)."
            
            VÍ DỤ TRẢ LỜI SAI - TUYỆT ĐỐI TRÁNH:
            ❌ "...đã bán được 150 sản phẩm" (khi dữ liệu chỉ có 25)
            ❌ "...được nhiều khách hàng tin dùng" (khi không có dữ liệu bán hàng)
            ❌ "...rating 4.8/5" (khi dữ liệu chỉ có 4.2/5)
            ❌ Bịa ra sản phẩm không có trong dữ liệu
            
            LƯU Ý ĐẶC BIỆT:
            - "Sản phẩm bán chạy": CHỈ xếp hạng theo số "Đã bán" trong dữ liệu
            - "Đánh giá cao": CHỈ xếp hạng theo số "Đánh giá" trong dữ liệu
            - "Giá rẻ/đắt": CHỈ so sánh "Giá" trong dữ liệu
            - Nếu danh sách sản phẩm RỖNG hoặc không có sản phẩm phù hợp: BÁO KHÔNG TÌM THẤY
            
            {products}
            """;

        String userPromptTemplate = """
            Câu hỏi khách hàng: {query}
            
            Intent phát hiện: {intents}
            
            ⚠️ LƯU Ý: Chỉ giới thiệu sản phẩm từ phần [DỮ LIỆU SẢN PHẨM THỰC TẾ - KẾT QUẢ LỌC] ở trên.
            Hãy tư vấn ngắn gọn, chuyên nghiệp, liệt kê 2-3 sản phẩm theo format đã chỉ định.
            """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("query", userQuery);
        variables.put("intents", intents.toString());

        // Structure the context to emphasize filtered products
        String structuredContext = String.format("""
            ═══════════════════════════════════════════════════════
            📦 [DỮ LIỆU SẢN PHẨM THỰC TẾ - KẾT QUẢ LỌC]
            ⚠️ QUAN TRỌNG: CHỈ GIỚI THIỆU CÁC SẢN PHẨM DƯỚI ĐÂY
            ═══════════════════════════════════════════════════════
            
            %s
            
            ═══════════════════════════════════════════════════════
            📊 [THỐNG KÊ TỔNG QUÁT - CHỈ THAM KHẢO]
            ⚠️ CHÚ Ý: Đây là thống kê chung, KHÔNG phải danh sách giới thiệu
            ═══════════════════════════════════════════════════════
            
            %s
            """, productContext, statisticsContext);

        variables.put("products", structuredContext);

        // Create prompt
        PromptTemplate promptTemplate = new PromptTemplate(
            systemPrompt + "\n\n" + userPromptTemplate);
        Prompt prompt = promptTemplate.create(variables);

        // Call LLM
        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
            .user(prompt.getContents())
            .call()
            .content();
    }
}

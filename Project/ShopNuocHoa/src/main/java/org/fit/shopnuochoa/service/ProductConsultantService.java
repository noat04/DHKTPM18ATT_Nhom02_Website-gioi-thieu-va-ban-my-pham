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
    private final ProductService productService;
    private final ProductVectorService vectorService;
    private final ProductFilterEngine filterEngine;

    /**
     * Main consultation method - Hybrid approach
     */
    public String consultProduct(String userQuery) {
        try {
            log.info("🔍 Starting Hybrid Consultation for query: {}", userQuery);

            // ========== PHASE 1: INTENT EXTRACTION ==========
            Map<String, Object> intents = vectorService.extractQueryIntents(userQuery);
            log.info("📊 Extracted intents: {}", intents);

            // ========== PHASE 2: STRUCTURED FILTERING ==========
            ProductFilterEngine.FilterCriteria criteria =
                filterEngine.buildCriteriaFromIntents(userQuery, intents);

            List<Product> filteredProducts = filterEngine.filterProducts(criteria);
            log.info("🔎 Filtered {} products", filteredProducts.size());

            // ========== PHASE 3: RAG - CONTEXT GENERATION ==========
            String productContext = vectorService.generateProductContext(filteredProducts);
            String statisticsContext = vectorService.generateStatisticsContext(
                productService.getAll());

            log.info("📚 Generated RAG context with {} products", filteredProducts.size());

            // ========== PHASE 4: LLM GENERATION ==========
            String enhancedResponse = generateLLMResponse(
                userQuery,
                productContext,
                statisticsContext,
                intents
            );

            log.info("✅ Consultation completed successfully");
            return enhancedResponse;

        } catch (Exception e) {
            log.error("❌ Error in hybrid consultation: ", e);
            return "Xin lỗi, đã có lỗi xảy ra khi tư vấn. Vui lòng thử lại sau.";
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
            
            NHIỆM VỤ:
            - Phân tích câu hỏi của khách hàng
            - Đưa ra gợi ý sản phẩm phù hợp từ danh sách có sẵn
            - Giải thích lý do tại sao gợi ý sản phẩm đó
            - Trả lời ngắn gọn, tự nhiên, thân thiện (2-4 câu)
            
            QUY TẮC:
            1. CHỈ giới thiệu sản phẩm có trong danh sách bên dưới
            2. Ưu tiên sản phẩm còn hàng, đánh giá cao
            3. Đề cập giá, NSX, đặc điểm nổi bật
            4. Không bịa đặt thông tin không có
            5. Nếu không tìm thấy sản phẩm phù hợp, gợi ý khách xem thêm
            
            {statistics}
            
            {products}
            """;

        String userPromptTemplate = """
            Câu hỏi khách hàng: {query}
            
            Intent phát hiện: {intents}
            
            Hãy tư vấn ngắn gọn, chuyên nghiệp.
            """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("query", userQuery);
        variables.put("products", productContext);
        variables.put("statistics", statisticsContext);
        variables.put("intents", intents.toString());

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

    /**
     * Get product recommendations based on user preferences
     */
    public List<Product> getRecommendations(String userQuery) {
        try {
            Map<String, Object> intents = vectorService.extractQueryIntents(userQuery);
            ProductFilterEngine.FilterCriteria criteria =
                filterEngine.buildCriteriaFromIntents(userQuery, intents);
            criteria.setLimit(5); // Top 5 recommendations

            return filterEngine.filterProducts(criteria);
        } catch (Exception e) {
            log.error("Error getting recommendations: ", e);
            return List.of();
        }
    }

    /**
     * Compare two products
     */
    public String compareProducts(Integer productId1, Integer productId2) {
        try {
            Product p1 = productService.getById(productId1);
            Product p2 = productService.getById(productId2);

            String comparisonContext = String.format("""
                So sánh hai sản phẩm:
                
                1. %s
                   - Giá: %,.0f VNĐ
                   - NSX: %s
                   - Đánh giá: %.1f/5 ⭐
                   - Dung tích: %s
                   
                2. %s
                   - Giá: %,.0f VNĐ
                   - NSX: %s
                   - Đánh giá: %.1f/5 ⭐
                   - Dung tích: %s
                """,
                p1.getName(), p1.getPrice(), p1.getCategory().getName(),
                p1.getAverageRating() != null ? p1.getAverageRating() : 0.0,
                p1.getVolume() != null ? p1.getVolume().name() : "N/A",
                p2.getName(), p2.getPrice(), p2.getCategory().getName(),
                p2.getAverageRating() != null ? p2.getAverageRating() : 0.0,
                p2.getVolume() != null ? p2.getVolume().name() : "N/A"
            );

            String prompt = comparisonContext +
                "\n\nHãy so sánh 2 sản phẩm này và đưa ra nhận xét ngắn gọn (3-4 câu).";

            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        } catch (Exception e) {
            log.error("Error comparing products: ", e);
            return "Không thể so sánh sản phẩm. Vui lòng thử lại.";
        }
    }

    /**
     * Get product details with AI-generated description
     */
    public String getProductInsights(Integer productId) {
        try {
            Product product = productService.getById(productId);
            List<Product> similar = vectorService.findSimilarProducts(
                product, productService.getAll(), 3);

            String context = String.format("""
                Thông tin sản phẩm:
                - Tên: %s
                - Giá: %,.0f VNĐ
                - NSX: %s
                - Đánh giá: %.1f/5 ⭐ (%d lượt)
                - Dung tích: %s
                - Giới tính: %s
                - Còn hàng: %s
                %s
                
                Sản phẩm tương tự: %s
                """,
                product.getName(),
                product.getPrice(),
                product.getCategory().getName(),
                product.getAverageRating() != null ? product.getAverageRating() : 0.0,
                product.getRatingCount() != null ? product.getRatingCount() : 0,
                product.getVolume() != null ? product.getVolume().name() : "N/A",
                product.getGender() != null ? product.getGender().name() : "N/A",
                product.isInStock() ? "✓ Còn " + product.getQuantity() : "✗ Hết hàng",
                product.getHotTrend() != null && product.getHotTrend() ? "- 🔥 HOT TREND" : "",
                similar.stream().map(Product::getName).reduce((a, b) -> a + ", " + b).orElse("Không có")
            );

            String prompt = context +
                "\n\nHãy mô tả ngắn gọn về sản phẩm này và ai nên mua (2-3 câu).";

            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        } catch (Exception e) {
            log.error("Error getting product insights: ", e);
            return "Không thể tạo thông tin chi tiết. Vui lòng thử lại.";
        }
    }
}


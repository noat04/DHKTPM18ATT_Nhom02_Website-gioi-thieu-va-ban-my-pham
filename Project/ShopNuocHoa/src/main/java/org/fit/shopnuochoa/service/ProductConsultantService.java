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
     * Main consultation method - Hybrid approach
     */
    public String consultProduct(String userQuery) {
        try {
            log.info("🔍 Starting Hybrid Consultation for query: {}", userQuery);

            // ========== PHASE 1: INTENT EXTRACTION ==========
            Map<String, Object> intents = vectorService.extractQueryIntents(userQuery);
            log.info("📊 Extracted intents: {}", intents);

            // ========== PHASE 2: STRUCTURED FILTERING ==========
            List<Product> filteredProducts;
            String productContext;

            // Xử lý đặc biệt cho query bán chạy
            if ((Boolean) intents.getOrDefault("isBestSelling", false)) {
                log.info("🏆 Processing best-selling query");
                List<ProductStatisticsService.ProductStats> bestSellers =
                    statisticsService.getBestSellingProducts(5);
                filteredProducts = bestSellers.stream()
                    .map(ProductStatisticsService.ProductStats::getProduct)
                    .collect(java.util.stream.Collectors.toList());
                // Sử dụng context đặc biệt cho bán chạy
                productContext = vectorService.generateBestSellingContext(bestSellers);
            }
            // Xử lý query hot trend
            else if ((Boolean) intents.getOrDefault("isHotTrend", false)) {
                log.info("🔥 Processing hot trend query");
                filteredProducts = statisticsService.getHotTrendProducts(5);
                productContext = vectorService.generateProductContext(filteredProducts);
            }
            // Xử lý query sản phẩm mới
            else if ((Boolean) intents.getOrDefault("isNewProducts", false)) {
                log.info("✨ Processing new products query");
                filteredProducts = statisticsService.getNewestProducts(5);
                productContext = vectorService.generateProductContext(filteredProducts);
            }
            // Xử lý query đánh giá cao
            else if ((Boolean) intents.getOrDefault("isTopRated", false)) {
                log.info("⭐ Processing top-rated query");
                filteredProducts = statisticsService.getTopRatedProducts(5);
                productContext = vectorService.generateProductContext(filteredProducts);
            }
            // Xử lý query giá rẻ
            else if ((Boolean) intents.getOrDefault("isCheapQuery", false)) {
                log.info("💰 Processing cheap products query");
                filteredProducts = statisticsService.getCheapestProducts(5);
                productContext = vectorService.generateProductContext(filteredProducts);
            }
            // Xử lý query giá đắt
            else if ((Boolean) intents.getOrDefault("isExpensiveQuery", false)) {
                log.info("💎 Processing expensive products query");
                filteredProducts = statisticsService.getMostExpensiveProducts(5);
                productContext = vectorService.generateProductContext(filteredProducts);
            }
            // Xử lý thông thường với filter engine
            else {
                ProductFilterEngine.FilterCriteria criteria =
                    filterEngine.buildCriteriaFromIntents(intents);
                filteredProducts = filterEngine.filterProducts(criteria);
                productContext = vectorService.generateProductContext(filteredProducts);
            }

            log.info("🔎 Filtered {} products", filteredProducts.size());

            // ========== PHASE 3: RAG - CONTEXT GENERATION ==========
            String statisticsContext = statisticsService.generateEnhancedStatistics();

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
            
            ⚠️ QUY TẮC BẮT BUỘC - KHÔNG ĐƯỢC VI PHẠM:
            1. ❌ TUYỆT ĐỐI KHÔNG bịa đặt, suy đoán, hoặc thêm thông tin không có trong dữ liệu
            2. ✅ CHỈ sử dụng CHÍNH XÁC thông tin từ [DỮ LIỆU SẢN PHẨM THỰC TẾ] bên dưới
            3. ✅ Nếu không có thông tin về một trường nào đó, hãy BỎ QUA, ĐỪNG đoán
            4. ✅ Số liệu "Đã bán" CHỈ lấy từ trường "Đã bán" trong dữ liệu
            5. ✅ Giá tiền, rating, tồn kho phải CHÍNH XÁC 100%
            6. ✅ Nếu không tìm thấy sản phẩm phù hợp, trả lời thẳng thắn
            
            CÁCH TRẢ LỜI:
            - Ngắn gọn (2-3 câu)
            - Tự nhiên, thân thiện
            - Đề cập: Tên sản phẩm, Giá, Thương hiệu
            - Nếu có "Đã bán": Nói rõ "Đã bán X sản phẩm"
            - Nếu KHÔNG có "Đã bán" hoặc = 0: ĐỪNG nói về số lượng bán
            
            VÍ DỤ TRẢ LỜI ĐÚNG:
            ✅ "Dior Sauvage (2,500,000 VNĐ) đã bán được 25 sản phẩm, là lựa chọn phổ biến."
            ✅ "Chanel Bleu (3,200,000 VNĐ) có đánh giá 4.5/5 sao, rất được ưa chuộng."
            ✅ "CK One (450,000 VNĐ) là lựa chọn giá tốt, còn 80 sản phẩm."
            
            VÍ DỤ TRẢ LỜI SAI - TUYỆT ĐỐI TRÁNH:
            ❌ "...đã bán được 150 sản phẩm" (khi dữ liệu chỉ có 25)
            ❌ "...được nhiều khách hàng tin dùng" (khi không có dữ liệu bán hàng)
            ❌ "...rating 4.8/5" (khi dữ liệu chỉ có 4.2/5)
            
            LƯU Ý ĐẶC BIỆT:
            - "Sản phẩm bán chạy": CHỈ xếp hạng theo số "Đã bán" trong dữ liệu
            - "Đánh giá cao": CHỈ xếp hạng theo số "Đánh giá" trong dữ liệu
            - "Giá rẻ/đắt": CHỈ so sánh "Giá" trong dữ liệu
            
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
}


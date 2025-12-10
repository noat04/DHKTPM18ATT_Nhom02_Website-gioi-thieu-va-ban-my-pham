package org.fit.shopnuochoa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Product;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hybrid LLM + Structured Filter + RAG Product Consultant Service
 *
 * ✨ FEATURES MỚI:
 * - Rate Limiting: Giới hạn 5 requests/phút/user để bảo vệ quota API
 * - Response Caching: Cache câu trả lời 5 phút để tăng tốc
 * - Retry Mechanism: Tự động retry khi gặp lỗi tạm thời
 * - Fallback Response: Trả về database khi API lỗi
 *
 * Architecture:
 * 1. Check Cache: Kiểm tra cache trước
 * 2. Check Rate Limit: Kiểm tra giới hạn request
 * 3. Intent Extraction: Phân tích câu hỏi người dùng
 * 4. Structured Filter: Lọc sản phẩm theo tiêu chí
 * 5. RAG: Tạo context từ database
 * 6. LLM Generation: Tạo câu trả lời tự nhiên (có retry)
 * 7. Update Cache: Lưu kết quả vào cache
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductConsultantService {

    private final ChatClient.Builder chatClientBuilder;
    private final ProductVectorService vectorService;
    private final ProductFilterEngine filterEngine;
    private final ProductStatisticsService statisticsService;

    // ========== RATE LIMITING ==========
    // Giới hạn: 5 requests/phút/user
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    // ========== RESPONSE CACHING ==========
    // Cache: 5 phút TTL, tối đa 100 entries
    private static final long CACHE_TTL_SECONDS = 300;
    private static final int MAX_CACHE_SIZE = 100;
    private final ConcurrentHashMap<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private final CategoryService categoryService;

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

    // ========== HELPER CLASSES FOR RATE LIMITING & CACHING ==========

    /**
     * Rate limit tracking per user
     */
    private static class RateLimitInfo {
        volatile long windowStart;
        final AtomicInteger requestCount;

        RateLimitInfo(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(0);
        }
    }

    /**
     * Cached response with timestamp
     */
    private static class CachedResponse {
        final ConsultationResult result;
        final long timestamp;

        CachedResponse(ConsultationResult result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }

    /**
     * Main consultation method with products - Returns both response and product list
     *
     * @param userQuery Câu hỏi của người dùng
     * @param userId ID người dùng (session ID hoặc IP) để rate limiting
     * @return ConsultationResult chứa response và danh sách sản phẩm
     */
    public ConsultationResult consultProductWithDetails(String userQuery, String userId) {
        // ========== DECLARE VARIABLES OUTSIDE TRY FOR FALLBACK ACCESS ==========
        List<Product> filteredProducts = List.of();
        String normalizedQuery = "";

        try {
            log.info("🔍 Starting Consultation for query: {} (userId: {})", userQuery, userId);

            // ========== STEP 1: CHECK CACHE ==========
            normalizedQuery = normalizeQuery(userQuery);
            ConsultationResult cachedResult = getFromCache(normalizedQuery);
            if (cachedResult != null) {
                log.info("✅ Returning CACHED response");
                return cachedResult;
            }

            // ========== STEP 2: CHECK RATE LIMIT ==========
            if (!checkRateLimit(userId)) {
                long waitTime = getWaitTime(userId);
                String rateLimitMsg = String.format(
                    "⏰ Bạn đã gửi quá nhiều tin nhắn. Vui lòng đợi %d giây trước khi tiếp tục.",
                    waitTime
                );
                log.warn("🚫 Rate limit exceeded for user: {}", userId);
                return new ConsultationResult(rateLimitMsg, List.of());
            }

            // ========== PHASE 3: INTENT EXTRACTION (REGEX ONLY - NO API CALL) ==========
            // ⚡ OPTIMIZATION: Dùng regex thay vì LLM để giảm API calls từ 2 → 1
            log.info("🧠 [STEP 3] Extracting intents using REGEX (no API call)...");
            Map<String, Object> intents = createFallbackIntents(userQuery);
            log.info("📊 Extracted intents: {}", intents);

            // ========== GREETING HANDLING ==========
            // Greeting không cần gọi API Gemini, trả về response cố định và CACHE
            if ((Boolean) intents.getOrDefault("isGreeting", false)) {
                log.info("👋 Greeting detected - returning cached response (no API call)");
                String greetingResponse = "Chào bạn! 👋 Tôi là trợ lý AI tư vấn nước hoa của ShopNuocHoa. Tôi có thể giúp bạn tìm nước hoa phù hợp, tư vấn giá cả, thương hiệu và nhiều hơn nữa. Bạn cần tôi giúp gì nhé?";

                // Cache greeting để lần sau không cần check intent nữa
                ConsultationResult greetingResult = new ConsultationResult(greetingResponse, List.of());
                saveToCache(normalizedQuery, greetingResult);

                return greetingResult;
            }

            // ========== PHASE 4: STRUCTURED FILTERING ==========
            log.info("🔍 [STEP 4] Filtering products from database...");

            // Build criteria from all intents without prioritization
            ProductFilterEngine.FilterCriteria criteria = filterEngine.buildCriteriaFromIntents(intents);
            log.info("📋 Filter criteria: minPrice={}, maxPrice={}, brand={}, gender={}, sortBy={}",
                criteria.getMinPrice(), criteria.getMaxPrice(), criteria.getCategoryName(),
                criteria.getGender(), criteria.getSortBy());

            filteredProducts = filterEngine.filterProducts(criteria);

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

            // ========== PHASE 5: LLM GENERATION ==========
            String enhancedResponse;
            try {
                log.info("💬 [STEP 5] Generating LLM response...");
                enhancedResponse = generateLLMResponse(
                    userQuery,
                    productContext,
                    statisticsContext,
                    intents
                );
            } catch (Exception llmError) {
                // Nếu LLM lỗi, log chi tiết và throw để fallback xử lý
                log.error("❌ LLM Generation failed: {}", llmError.getMessage());
                log.debug("LLM Error details:", llmError);
                throw llmError; // Re-throw để trigger fallback
            }

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

            // ========== STEP 6: SAVE TO CACHE ==========
            ConsultationResult result = new ConsultationResult(enhancedResponse, productsToReturn);
            saveToCache(normalizedQuery, result);

            return result;

        } catch (Exception e) {
            log.error("❌ Error in consultation: ", e);

            // ========== FALLBACK: TRẢ VỀ RESPONSE CƠ BẢN ==========
            // ⚠️ QUAN TRỌNG: KHÔNG GỌI extractQueryIntents() LẠI NỮA
            // Sử dụng filteredProducts đã có hoặc tạo fallback response đơn giản
            log.warn("⚠️ Using FALLBACK mode due to API error: {}", e.getMessage());

            try {
                // KHÔNG gọi vectorService.extractQueryIntents() nữa!
                // Dùng filteredProducts có sẵn từ scope trên

                String fallbackMsg;
                if (filteredProducts == null || filteredProducts.isEmpty()) {
                    // Không tìm thấy sản phẩm - Thông báo thân thiện
                    fallbackMsg = "Chào bạn, hiện tại tôi không tìm thấy sản phẩm phù hợp với yêu cầu của bạn. Bạn có thể thử tìm kiếm với từ khóa khác hoặc xem các sản phẩm nổi bật của chúng tôi nhé! 😊";
                } else {
                    // Có sản phẩm - Thông báo tự nhiên KHÔNG nhắc đến lỗi
                    fallbackMsg = String.format(
                        "Chào bạn, tôi đã tìm được %d sản phẩm có thể phù hợp với bạn:",
                        Math.min(3, filteredProducts.size())
                    );
                    filteredProducts = filteredProducts.stream().limit(3).collect(java.util.stream.Collectors.toList());
                }

                log.info("🛡️ Fallback response generated with {} products",
                        filteredProducts != null ? filteredProducts.size() : 0);

                return new ConsultationResult(fallbackMsg,
                        filteredProducts != null ? filteredProducts : List.of());

            } catch (Exception fallbackError) {
                log.error("❌ Fallback also failed: ", fallbackError);
                // Thông báo cuối cùng khi mọi thứ đều thất bại
                return new ConsultationResult(
                    "Xin lỗi, hệ thống đang bận. Vui lòng thử lại sau giây lát hoặc liên hệ bộ phận hỗ trợ. Cảm ơn bạn! 🙏",
                    List.of()
                );
            }
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
            Bạn là chuyên gia tư vấn nước hoa tại ShopNuocHoa với 10 năm kinh nghiệm. 
            Phong cách của bạn: thân thiện, chuyên nghiệp, nhiệt tình như đang tư vấn trực tiếp cho khách hàng.
            
            ═══════════════════════════════════════════════════════
            🎯 NHIỆM VỤ CỦA BẠN
            ═══════════════════════════════════════════════════════
            
            Tư vấn sản phẩm dựa trên [DỮ LIỆU SẢN PHẨM] bên dưới - đây là những sản phẩm 
            đã được HỆ THỐNG LỌC SẴN theo yêu cầu của khách hàng.
            
            ═══════════════════════════════════════════════════════
            ✅ CÁCH TƯ VẤN TỰ NHIÊN VÀ CHUYÊN NGHIỆP
            ═══════════════════════════════════════════════════════
            
            1. 💬 GIỌNG ĐIỆU:
               • Thân thiện, ấm áp như đang nói chuyện trực tiếp
               • Dùng "mình", "bạn" để gần gũi hơn
               • Nhiệt tình nhưng không quá thương mại
               • Tự nhiên, không cứng nhắc
            
            2. 🎨 CẤU TRÚC CÂU TRẢ LỜI:
            
               📌 Nếu tìm thấy 1 sản phẩm (câu hỏi "nhất"):
               "Dạ, với [yêu cầu] thì mình gợi ý cho bạn [Tên SP] của [Brand] nhé! 
               Sản phẩm này [điểm nổi bật], giá [X] VNĐ, hiện đang [tình trạng]. 
               [Thêm 1-2 câu mô tả ngắn nếu có]"
               
               📌 Nếu tìm thấy 2-3 sản phẩm:
               "Dạ, mình có mấy gợi ý phù hợp với bạn này:
               
               🌸 [Tên SP1] - [Brand]
               → [Điểm nổi bật], giá [X] VNĐ
               
               🌸 [Tên SP2] - [Brand]  
               → [Điểm nổi bật], giá [X] VNĐ
               
               [Câu kết: gợi ý thêm hoặc hỏi thêm]"
               
               📌 Nếu không tìm thấy:
               "Dạ, rất tiếc là hiện tại shop chưa có sản phẩm [yêu cầu] ạ. 
               Bạn có thể thử tìm với [gợi ý khác] hoặc xem các sản phẩm [tương tự] nhé!"
            
            3. 🎁 ĐIỂM NỔI BẬT CẦN NHỚ:
               • Nếu "Đã bán > 0": "đã có [X] khách hàng tin dùng"
               • Nếu "HOT TREND": "đang rất được yêu thích"
               • Nếu "Rating cao": "được đánh giá [X]/5 sao"
               • Nếu "Tồn kho ít": "số lượng có hạn"
               • Nếu giá cao: "cao cấp", "sang trọng"
               • Nếu giá thấp: "giá tốt", "phù hợp túi tiền"
            
            4. ❌ TUYỆT ĐỐI TRÁNH:
               • Bịa thông tin không có trong dữ liệu
               • Nói "đã bán được X" khi không có dữ liệu
               • Copy nguyên văn format cứng nhắc
               • Quá dài dòng, lan man
               • Giới thiệu sản phẩm KHÔNG CÓ trong danh sách
            
            5. ✨ MẸO TƯ VẤN HAY:
               • Dùng emoji phù hợp (🌸 💖 ✨ 🎁) nhưng đừng lạm dụng
               • Kết thúc bằng câu hỏi mở để tiếp tục hội thoại
               • Thêm insight nhỏ về mùi hương nếu có info
               • Gợi ý cách sử dụng hoặc dịp phù hợp
            
            ═══════════════════════════════════════════════════════
            📋 VÍ DỤ CÂU TRẢ LỜI TỐT
            ═══════════════════════════════════════════════════════
            
            ✅ VÍ DỤ 1 (Tìm sản phẩm đắt nhất):
            "Dạ, sản phẩm cao cấp nhất của Dior mình có là J'adore Eau de Parfum nhé, 
            giá 3.800.000 VNĐ. Đây là dòng nước hoa sang trọng, mùi hương quyến rũ và 
            lưu hương cực tốt ạ. Bạn quan tâm mình tư vấn thêm không?"
            
            ✅ VÍ DỤ 2 (Gợi ý nhiều sản phẩm):
            "Dạ, với mức giá này mình gợi ý cho bạn 3 lựa chọn hay ho này:
            
            🌸 Dior Sauvage - 2.500.000đ
            → Mùi hương nam tính, tươi mát, đang rất hot với hơn 150 bạn đã mua ạ
            
            🌸 Chanel Bleu - 3.200.000đ  
            → Thanh lịch, sang trọng, được đánh giá 4.5/5 sao
            
            🌸 CK One - 450.000đ
            → Unisex, giá sinh viên, phù hợp dùng hàng ngày
            
            Bạn thích hương nào mạnh mẽ hay nhẹ nhàng để mình tư vấn kỹ hơn nhé?"
            
            ✅ VÍ DỤ 3 (Không tìm thấy):
            "Dạ, rất tiếc là hiện tại shop chưa có sản phẩm trong khoảng giá 500-800k ạ. 
            Bạn có thể xem các sản phẩm giá tốt dưới 500k hoặc từ 1 triệu trở lên nhé. 
            Mình tư vấn thêm không?"
            
            ═══════════════════════════════════════════════════════
            ⚠️ QUY TẮC NGHIÊM NGẶT
            ═══════════════════════════════════════════════════════
            
            1. CHỈ giới thiệu sản phẩm từ [DỮ LIỆU SẢN PHẨM] bên dưới
            2. Giá, tồn kho, đã bán phải CHÍNH XÁC 100%
            3. Nếu không có dữ liệu → Không nhắc đến thông tin đó
            4. Phần [THỐNG KÊ] chỉ để tham khảo, KHÔNG phải danh sách giới thiệu
            5. Tự nhiên, thân thiện nhưng vẫn chính xác về mặt thông tin
            
            {products}
            """;

        String userPromptTemplate = """
            ═══════════════════════════════════════════════════════
            💬 YÊU CẦU TƯ VẤN
            ═══════════════════════════════════════════════════════
            
            Câu hỏi: "{query}"
            
            Phân tích nhanh: {intents}
            
            ⚠️ Lưu ý: Chỉ giới thiệu sản phẩm từ [DỮ LIỆU SẢN PHẨM] ở trên.
            
            Hãy tư vấn tự nhiên, thân thiện như đang nói chuyện trực tiếp với khách hàng nhé! 😊
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

    // ========== RATE LIMITING METHODS ==========

    /**
     * Kiểm tra xem user có được phép gửi request không (rate limit check)
     *
     * @param userId ID của user
     * @return true nếu được phép, false nếu vượt quota
     */
    private boolean checkRateLimit(String userId) {
        long currentTime = Instant.now().getEpochSecond();

        RateLimitInfo rateLimitInfo = rateLimitMap.computeIfAbsent(
            userId,
            k -> new RateLimitInfo(currentTime)
        );

        // Kiểm tra xem window đã hết hạn chưa
        if (currentTime - rateLimitInfo.windowStart >= RATE_LIMIT_WINDOW_SECONDS) {
            // Reset window mới
            rateLimitInfo.windowStart = currentTime;
            rateLimitInfo.requestCount.set(0);
        }

        // Kiểm tra số lượng request
        int currentCount = rateLimitInfo.requestCount.get();

        if (currentCount >= MAX_REQUESTS_PER_MINUTE) {
            return false; // Vượt quota
        }

        // Tăng counter và cho phép
        rateLimitInfo.requestCount.incrementAndGet();
        return true;
    }

    /**
     * Lấy thời gian phải chờ (giây) trước khi có thể request lại
     *
     * @param userId ID của user
     * @return số giây phải chờ
     */
    private long getWaitTime(String userId) {
        RateLimitInfo info = rateLimitMap.get(userId);
        if (info == null) {
            return 0;
        }

        long currentTime = Instant.now().getEpochSecond();
        long elapsedTime = currentTime - info.windowStart;

        if (elapsedTime >= RATE_LIMIT_WINDOW_SECONDS) {
            return 0;
        }

        return RATE_LIMIT_WINDOW_SECONDS - elapsedTime;
    }

    // ========== CACHING METHODS ==========

    /**
     * Lấy response từ cache (nếu có và còn hạn)
     *
     * @param normalizedQuery Câu hỏi đã normalize
     * @return ConsultationResult nếu có trong cache, null nếu không
     */
    private ConsultationResult getFromCache(String normalizedQuery) {
        CachedResponse cached = responseCache.get(normalizedQuery);

        if (cached != null) {
            long currentTime = Instant.now().getEpochSecond();

            if (currentTime - cached.timestamp < CACHE_TTL_SECONDS) {
                log.info("💾 Cache HIT for query: {}", normalizedQuery);
                return cached.result;
            } else {
                // Cache hết hạn - xóa đi
                responseCache.remove(normalizedQuery);
                log.info("⏰ Cache EXPIRED for query: {}", normalizedQuery);
            }
        }

        log.info("❌ Cache MISS for query: {}", normalizedQuery);
        return null;
    }

    /**
     * Lưu response vào cache
     *
     * @param normalizedQuery Câu hỏi đã normalize
     * @param result Kết quả cần cache
     */
    private void saveToCache(String normalizedQuery, ConsultationResult result) {
        // Kiểm tra kích thước cache
        if (responseCache.size() >= MAX_CACHE_SIZE) {
            cleanupOldestCacheEntries(10);
        }

        long currentTime = Instant.now().getEpochSecond();
        responseCache.put(normalizedQuery, new CachedResponse(result, currentTime));
        log.info("💾 Saved to cache: {}", normalizedQuery);
    }

    /**
     * Xóa các cache entries cũ nhất
     *
     * @param count Số lượng entries cần xóa
     */
    private void cleanupOldestCacheEntries(int count) {
        log.info("🧹 Cleaning up {} oldest cache entries", count);

        responseCache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
            .limit(count)
            .forEach(entry -> responseCache.remove(entry.getKey()));
    }

    /**
     * Normalize câu hỏi để tăng cache hit rate
     *
     * @param query Câu hỏi gốc
     * @return Câu hỏi đã normalize (lowercase, trim, loại bỏ dấu câu)
     */
    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.toLowerCase()
                    .trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[?!.,;]+$", "");
    }

    /**
     * Tạo intent bằng regex pattern matching (KHÔNG gọi API)
     *
     * ⚡ OPTIMIZATION: Thay thế LLM extraction để giảm API calls từ 2 → 1
     *
     * @param userQuery Câu hỏi người dùng
     * @return Map chứa intent đầy đủ
     */
    private Map<String, Object> createFallbackIntents(String userQuery) {
        Map<String, Object> intents = new HashMap<>();
        String queryLower = userQuery.toLowerCase();

        // ========== GREETING ==========
        intents.put("isGreeting",
            queryLower.matches("^(xin chào|chào|hello|hi|hey|chào bạn|chào shop).*"));

        // ========== PRICE QUERIES ==========
        intents.put("isPriceQuery",
            queryLower.contains("giá") || queryLower.contains("price") ||
            queryLower.matches(".*(bao nhiêu|mắc|rẻ|đắt).*"));

        // Cheap/Student-friendly
        intents.put("isCheapQuery",
            queryLower.matches(".*(rẻ|giá tốt|sinh viên|học sinh|bình dân|giá mềm|giá rẻ|dưới|duoi).*"));

        // Expensive/Luxury
        intents.put("isExpensiveQuery",
            queryLower.matches(".*(đắt|mắc|cao cấp|sang trọng|giá cao|trên|tren|đắt nhất|mắc nhất).*"));

        // ========== PRODUCT QUERIES ==========
        // Best selling
        intents.put("isBestSelling",
            queryLower.matches(".*(bán chạy|phổ biến|bán nhiều|được ưa chuộng|bán chạy nhất).*"));

        // Hot trend
        intents.put("isHotTrend",
            queryLower.matches(".*(hot trend|thịnh hành|hot|xu hướng|trend).*"));

        // Recommendation
        intents.put("isRecommendation",
            queryLower.matches(".*(gợi ý|tư vấn|đề xuất|giúp|help|recommend).*"));

        // Top rated
        intents.put("isTopRated",
            queryLower.matches(".*(đánh giá cao|rating cao|tốt nhất|chất lượng).*"));

        // New products
        intents.put("isNewProducts",
            queryLower.matches(".*(mới|mới nhất|ra mắt|new).*"));

        // ========== GENDER ==========
        String gender = null;
        if (queryLower.matches(".*(nam|cho nam|dành cho nam|nam giới|men).*")) {
            gender = "NAM";
        } else if (queryLower.matches(".*(nữ|nu|cho nữ|dành cho nữ|nữ giới|women).*")) {
            gender = "NU";
        } else if (queryLower.matches(".*(unisex|cả nam và nữ).*")) {
            gender = "UNISEX";
        }
        intents.put("gender", gender);

        // ========== BRAND EXTRACTION ==========
        // Common brands
        String brandKeyword = null;
        List<Category> brands = categoryService.getAll();

        for (Category brand : brands) {
            if (brand.getName() != null && queryLower.contains(brand.getName().toLowerCase())) {
                brandKeyword = brand.getName().toLowerCase();
                break;
            }
        }
        intents.put("brandKeyword", brandKeyword);
        intents.put("brand", brandKeyword);

        // ========== PRICE EXTRACTION ==========
        Double minPrice = null;
        Double maxPrice = null;

        // Pattern: "dưới 500k", "duoi 1 triệu", "< 2tr"
        if (queryLower.matches(".*(dưới|duoi|<|thấp hơn|ít hơn)\\s*(\\d+).*")) {
            String priceStr = queryLower.replaceAll(".*(dưới|duoi|<|thấp hơn|ít hơn)\\s*(\\d+).*", "$2");
            try {
                double price = Double.parseDouble(priceStr);
                // Check unit
                if (queryLower.contains("triệu") || queryLower.contains("tr")) {
                    maxPrice = price * 1000000;
                } else if (queryLower.contains("k") || queryLower.contains("nghìn")) {
                    maxPrice = price * 1000;
                } else {
                    maxPrice = price;
                }
            } catch (Exception ignored) {}
        }

        // Pattern: "trên 1 triệu", "tren 500k", "> 2tr"
        if (queryLower.matches(".*(trên|tren|>|cao hơn|nhiều hơn)\\s*(\\d+).*")) {
            String priceStr = queryLower.replaceAll(".*(trên|tren|>|cao hơn|nhiều hơn)\\s*(\\d+).*", "$2");
            try {
                double price = Double.parseDouble(priceStr);
                if (queryLower.contains("triệu") || queryLower.contains("tr")) {
                    minPrice = price * 1000000;
                } else if (queryLower.contains("k") || queryLower.contains("nghìn")) {
                    minPrice = price * 1000;
                } else {
                    minPrice = price;
                }
            } catch (Exception ignored) {}
        }

        // Pattern: "từ 1 đến 2 triệu", "1-2tr", "1tr - 2tr"
        if (queryLower.matches(".*(từ|tu)\\s*(\\d+).*(?:đến|den|-|->)\\s*(\\d+).*")) {
            // Extract min and max from range
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+).*(?:đến|den|-|->)\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(queryLower);
            if (matcher.find()) {
                try {
                    double min = Double.parseDouble(matcher.group(1));
                    double max = Double.parseDouble(matcher.group(2));
                    if (queryLower.contains("triệu") || queryLower.contains("tr")) {
                        minPrice = min * 1000000;
                        maxPrice = max * 1000000;
                    } else if (queryLower.contains("k") || queryLower.contains("nghìn")) {
                        minPrice = min * 1000;
                        maxPrice = max * 1000;
                    } else {
                        minPrice = min;
                        maxPrice = max;
                    }
                } catch (Exception ignored) {}
            }
        }

        intents.put("minPrice", minPrice);
        intents.put("maxPrice", maxPrice);

        // ========== PRODUCT NAME ==========
        // Check if asking about specific product with multiple patterns
        boolean isProductNameQuery = false;
        String productName = null;

        // Pattern 1: "nước hoa [tên]", "perfume [tên]"
        if (queryLower.matches(".*(nước hoa|perfume)\\s+([a-zA-Z0-9\\s]+).*")) {
            isProductNameQuery = true;
            productName = queryLower.replaceAll(".*(nước hoa|perfume)\\s+([a-zA-Z0-9\\s]+?)(?:\\s|$).*", "$2").trim();
        }
        // Pattern 2: "sản phẩm [tên]", "sp [tên]", "product [tên]"
        else if (queryLower.matches(".*(sản phẩm|san pham|sp|product)\\s+([a-zA-Z0-9\\s]+).*")) {
            isProductNameQuery = true;
            productName = queryLower.replaceAll(".*(sản phẩm|san pham|sp|product)\\s+([a-zA-Z0-9\\s]+).*", "$2").trim();
        }
        // Pattern 3: Tìm chính xác tên (không có keyword)
        else if (!queryLower.matches("^(xin chào|chào|hello|hi|giá|rẻ|đắt|bán chạy|hot).*") &&
                 queryLower.length() > 3 && queryLower.matches("^[a-zA-Z0-9\\s]+$")) {
            isProductNameQuery = true;
            productName = queryLower.trim();
        }

        // Clean up product name (remove trailing words)
        if (productName != null && !productName.isEmpty()) {
            // Remove common trailing words
            productName = productName.replaceAll("\\s+(là gì|nào|thế nào|như thế nào|không|có|à|ạ).*", "").trim();
            // Limit length to avoid too long queries
            if (productName.split("\\s+").length > 5) {
                productName = String.join(" ", java.util.Arrays.copyOfRange(productName.split("\\s+"), 0, 5));
            }
        }

        intents.put("isProductNameQuery", isProductNameQuery);
        intents.put("productName", productName);

        // ========== OTHER INTENTS ==========
        intents.put("isComparison", queryLower.matches(".*(so sánh|compare|khác nhau).*"));
        intents.put("isAvailability", queryLower.matches(".*(còn hàng|có hàng|availability|stock).*"));

        log.info("🔧 Created intents using REGEX (no API call): {}", intents);
        return intents;
    }
}

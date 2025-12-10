package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fit.shopnuochoa.dto.ChatRequest;
import org.fit.shopnuochoa.dto.ChatResponse;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.service.ProductConsultantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consultant")
@RequiredArgsConstructor
@Slf4j
public class ProductConsultantController {

    private final ProductConsultantService consultantService;

    /**
     * Main chat endpoint - Hybrid LLM + Filter + RAG với Rate Limiting & Caching
     *
     * @param request ChatRequest chứa message của user
     * @param httpRequest HttpServletRequest để lấy session ID
     * @return ChatResponse chứa response và danh sách sản phẩm
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        try {
            // ========== LẤY USER ID TỪ SESSION ==========
            // Sử dụng session ID làm user identifier cho rate limiting
            String userId = httpRequest.getSession().getId();

            // Nếu không có session, dùng IP address
            if (userId == null || userId.isEmpty()) {
                userId = httpRequest.getRemoteAddr();
            }

            log.info("📨 Chat request from user: {} (session: {})", userId, httpRequest.getSession().getId());

            // ========== GỌI SERVICE VỚI USER ID ==========
            ProductConsultantService.ConsultationResult result =
                consultantService.consultProductWithDetails(request.getMessage(), userId);

            // Convert products to DTOs (limit to 3)
            List<ChatResponse.ProductCardDTO> productDTOs = result.getProducts().stream()
                .limit(3)
                .map(this::convertToProductCard)
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ChatResponse(result.getResponse(), productDTOs));

        } catch (Exception e) {
            log.error("❌ Error in consultation: {}", e.getMessage(), e);

            // ========== TRẢ VỀ THÔNG BÁO LỖI THÂN THIỆN ==========
            String errorMessage;
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                errorMessage = "Xin lỗi, hệ thống AI tạm thời đã đạt giới hạn sử dụng. Vui lòng thử lại sau ít phút hoặc liên hệ bộ phận hỗ trợ.";
            } else if (e.getMessage() != null && e.getMessage().contains("503")) {
                errorMessage = "Xin lỗi, dịch vụ AI đang quá tải. Vui lòng thử lại sau.";
            } else {
                errorMessage = "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.";
            }

            return ResponseEntity.ok()
                    .body(new ChatResponse(errorMessage, null));
        }
    }

    /**
     * Convert Product entity to ProductCardDTO
     */
    private ChatResponse.ProductCardDTO convertToProductCard(Product product) {
        return new ChatResponse.ProductCardDTO(
            product.getId().longValue(),
            product.getName(),
            product.getPrice(),
            product.getImageUrl(),
            product.getCategory() != null ? product.getCategory().getName() : "",
            product.getAverageRating(),
            product.getRatingCount(),
            product.getHotTrend()
        );
    }
}

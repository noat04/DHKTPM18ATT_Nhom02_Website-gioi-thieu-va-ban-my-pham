package org.fit.shopnuochoa.controller;

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
     * Main chat endpoint - Hybrid LLM + Filter + RAG
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            ProductConsultantService.ConsultationResult result =
                consultantService.consultProductWithDetails(request.getMessage());

            // Convert products to DTOs (limit to 3)
            List<ChatResponse.ProductCardDTO> productDTOs = result.getProducts().stream()
                .limit(3)
                .map(this::convertToProductCard)
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ChatResponse(result.getResponse(), productDTOs));
        } catch (Exception e) {
            log.error("❌ Error in consultation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.", null));
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

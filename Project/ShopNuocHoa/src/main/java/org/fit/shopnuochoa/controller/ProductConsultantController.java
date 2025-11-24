package org.fit.shopnuochoa.controller;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.dto.ChatRequest;
import org.fit.shopnuochoa.dto.ChatResponse;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.service.ProductConsultantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultant")
@RequiredArgsConstructor
public class ProductConsultantController {

    private final ProductConsultantService consultantService;

    /**
     * Main chat endpoint - Hybrid LLM + Filter + RAG
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {

        try {
            String response = consultantService.consultProduct(request.getMessage());
            return ResponseEntity.ok(new ChatResponse(response));
        } catch (Exception e) {
            System.err.println("❌ Error in consultation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }

    /**
     * Get personalized recommendations based on query
     */
    @PostMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(@RequestBody ChatRequest request) {
        try {
            List<Product> recommendations = consultantService.getRecommendations(request.getMessage());
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Không thể tạo gợi ý. Vui lòng thử lại."));
        }
    }

    /**
     * Compare two products using AI
     */
    @GetMapping("/compare/{id1}/{id2}")
    public ResponseEntity<ChatResponse> compareProducts(
            @PathVariable Integer id1,
            @PathVariable Integer id2) {
        try {
            String comparison = consultantService.compareProducts(id1, id2);
            return ResponseEntity.ok(new ChatResponse(comparison));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Không thể so sánh sản phẩm."));
        }
    }

    /**
     * Get AI-generated insights for a product
     */
    @GetMapping("/insights/{productId}")
    public ResponseEntity<ChatResponse> getProductInsights(@PathVariable Integer productId) {
        try {
            String insights = consultantService.getProductInsights(productId);
            return ResponseEntity.ok(new ChatResponse(insights));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Không thể tạo thông tin chi tiết."));
        }
    }
}

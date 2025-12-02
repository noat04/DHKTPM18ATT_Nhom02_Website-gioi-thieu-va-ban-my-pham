package org.fit.shopnuochoa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fit.shopnuochoa.dto.ChatRequest;
import org.fit.shopnuochoa.dto.ChatResponse;
import org.fit.shopnuochoa.service.ProductConsultantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            String response = consultantService.consultProduct(request.getMessage());
            return ResponseEntity.ok(new ChatResponse(response));
        } catch (Exception e) {
            log.error("❌ Error in consultation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }
}

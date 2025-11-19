package org.fit.shopnuochoa.controller;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.dto.ChatRequest;
import org.fit.shopnuochoa.dto.ChatResponse;
import org.fit.shopnuochoa.service.ProductConsultantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consultant")
@RequiredArgsConstructor
public class ProductConsultantController {

    private final ProductConsultantService consultantService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        System.out.println("=== ProductConsultantController.chat() called ===");
        System.out.println("Received message: " + request.getMessage());

        try {
            String response = consultantService.consultProduct(request.getMessage());
            System.out.println("Service returned response: " + response);

            ChatResponse chatResponse = new ChatResponse(response);
            System.out.println("Sending ChatResponse: " + chatResponse);

            return ResponseEntity.ok(chatResponse);
        } catch (Exception e) {
            System.err.println("Exception in controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau."));
        }
    }
}


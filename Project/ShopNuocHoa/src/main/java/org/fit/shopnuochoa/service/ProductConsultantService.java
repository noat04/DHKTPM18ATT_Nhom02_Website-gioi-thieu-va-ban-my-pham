package org.fit.shopnuochoa.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductConsultantService {

    private final ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        // Khởi tạo ChatClient với system prompt ngắn gọn
        String systemPrompt = """
                Bạn là trợ lý tư vấn nước hoa. Trả lời NGẮN GỌN (2-3 câu), đi thẳng vào vấn đề.
                
                Khi tư vấn:
                - Gợi ý 1-2 sản phẩm cụ thể
                - Nêu mùi hương chính
                - Phù hợp với ai/dịp gì
                
                KHÔNG giải thích dài dòng. KHÔNG liệt kê nhiều mục. CHỈ trả lời điểm chính.
                """;


        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .build();
    }

    public String consultProduct(String userMessage) {
        try {
            System.out.println("=== Consulting product with message: " + userMessage);

            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            System.out.println("=== Response received from AI");
            return response;

        } catch (Exception e) {
            System.err.println("Error consulting product: " + e.getMessage());
            e.printStackTrace();
            return "Xin lỗi, đã có lỗi xảy ra khi tư vấn. Vui lòng thử lại sau.";
        }
    }
}
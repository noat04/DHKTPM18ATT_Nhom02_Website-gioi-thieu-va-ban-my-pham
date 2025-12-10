package org.fit.shopnuochoa.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class GeminiChatModel implements ChatModel {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ========== RETRY CONFIGURATION ==========
    // Retry khi gặp lỗi 429 (Too Many Requests) hoặc 503 (Service Unavailable)
    private static final int MAX_RETRY_ATTEMPTS = 3;           // Tối đa retry 3 lần
    private static final long INITIAL_RETRY_DELAY_MS = 1000;   // Delay ban đầu: 1 giây
    private static final double RETRY_DELAY_MULTIPLIER = 2.0;  // Nhân đôi mỗi lần retry

    public GeminiChatModel() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        // ========== IDENTIFY CALL TYPE ==========
        // Xác định xem đây là intent extraction hay response generation
        String promptText = prompt.getInstructions().get(0).getContent();
        String callType = promptText.contains("trích xuất tiêu chí") || promptText.contains("INTENT")
            ? "[INTENT EXTRACTION]"
            : "[RESPONSE GENERATION]";

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🎯 " + callType + " Starting API Call");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ========== RETRY MECHANISM với EXPONENTIAL BACKOFF ==========
        int attempts = 0;
        long retryDelay = INITIAL_RETRY_DELAY_MS;
        Exception lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                attempts++;
                System.out.println("🔄 " + callType + " Attempt " + attempts + "/" + MAX_RETRY_ATTEMPTS);

                return executeApiCall(prompt);

            } catch (Exception e) {
                lastException = e;
                String errorMsg = e.getMessage();

                // Log chi tiết lỗi
                System.err.println("⚠️ " + callType + " API Error: " + errorMsg);

                // Kiểm tra xem có phải lỗi cần retry không
                boolean shouldRetry = errorMsg.contains("429") ||     // Too Many Requests
                                     errorMsg.contains("503") ||     // Service Unavailable
                                     errorMsg.contains("RESOURCE_EXHAUSTED") ||
                                     errorMsg.contains("overloaded");

                if (!shouldRetry || attempts >= MAX_RETRY_ATTEMPTS) {
                    if (!shouldRetry) {
                        System.err.println("❌ " + callType + " Non-retryable error - stopping");
                    } else {
                        System.err.println("❌ " + callType + " Max retry attempts reached (" + attempts + "/" + MAX_RETRY_ATTEMPTS + ")");
                        if (errorMsg.contains("429")) {
                            System.err.println("💡 TIP: API quota exhausted. Wait 1 minute or create new API key at: https://aistudio.google.com/apikey");
                        } else {
                            System.err.println("💡 TIP: Gemini server is overloaded. Wait 1-2 minutes and try again.");
                        }
                    }
                    throw e;
                }

                // ========== PARSE RETRY DELAY TỪ API RESPONSE ==========
                // Google API trả về retry delay trong error message
                long suggestedDelay = retryDelay;
                try {
                    // Tìm "Please retry in XX.XXs" trong error message
                    if (errorMsg.contains("Please retry in")) {
                        String delayStr = errorMsg.substring(errorMsg.indexOf("Please retry in") + 16);
                        delayStr = delayStr.substring(0, delayStr.indexOf("s"));
                        double delaySec = Double.parseDouble(delayStr);
                        suggestedDelay = (long)(delaySec * 1000); // Convert to milliseconds
                        System.out.println("📌 " + callType + " Google suggests retry in: " + delaySec + "s");
                    }
                } catch (Exception parseError) {
                    // Nếu parse lỗi, dùng exponential backoff mặc định
                }

                // Xác định loại lỗi để log cho rõ
                String errorType = "Unknown";
                if (errorMsg.contains("429")) errorType = "429 Too Many Requests";
                else if (errorMsg.contains("503")) errorType = "503 Service Unavailable";
                else if (errorMsg.contains("RESOURCE_EXHAUSTED")) errorType = "Resource Exhausted";
                else if (errorMsg.contains("overloaded")) errorType = "Server Overloaded";

                // Log và chờ trước khi retry
                System.out.println("⚠️ " + callType + " Error: " + errorType + " - Retrying in " + suggestedDelay + "ms... (Attempt " + attempts + "/" + MAX_RETRY_ATTEMPTS + ")");

                try {
                    Thread.sleep(suggestedDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                // Exponential backoff: Tăng thời gian chờ cho lần sau
                retryDelay = (long)(retryDelay * RETRY_DELAY_MULTIPLIER);
            }
        }

        // Nếu hết số lần retry, throw exception cuối cùng
        System.err.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.err.println("❌ " + callType + " FAILED after " + MAX_RETRY_ATTEMPTS + " attempts");
        System.err.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        throw new RuntimeException("Error calling Gemini API after " + MAX_RETRY_ATTEMPTS +
                                 " attempts: " + (lastException != null ? lastException.getMessage() : "Unknown error"),
                                 lastException);
    }

    /**
     * Thực hiện API call thực tế đến Gemini
     *
     * @param prompt Prompt từ Spring AI
     * @return ChatResponse
     */
    private ChatResponse executeApiCall(Prompt prompt) {
        try {
            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();

            // Add messages from prompt
            prompt.getInstructions().forEach(message -> {
                Map<String, Object> content = new HashMap<>();
                Map<String, String> part = new HashMap<>();
                part.put("text", message.getContent());
                content.put("parts", List.of(part));
                contents.add(content);
            });

            requestBody.put("contents", contents);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call Gemini API using baseUrl from config
            String url = baseUrl + model + ":generateContent?key=" + apiKey;

            // Debugging API key (CHỈ để debug, NÊN XÓA trong production)
            // System.out.println("Using API Key: " + apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // Parse response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");

                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String text = parts.get(0).path("text").asText();

                        // Create AssistantMessage from text
                        AssistantMessage assistantMessage = new AssistantMessage(text);
                        Generation generation = new Generation(assistantMessage);
                        return new ChatResponse(List.of(generation));
                    }
                }
            }

            throw new RuntimeException("Failed to get valid response from Gemini");

        } catch (Exception e) {
            // Re-throw để retry mechanism xử lý
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;  // No specific options needed
    }
}

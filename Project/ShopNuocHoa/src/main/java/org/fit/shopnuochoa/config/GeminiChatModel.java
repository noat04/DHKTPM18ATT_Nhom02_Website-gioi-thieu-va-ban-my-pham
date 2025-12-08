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

    @Value("AIzaSyB0becCvvDriWGBwFCfGLkJFGo7UdrKM44")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiChatModel() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
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

            // Debugging API key
            System.out.println("Using API Key: " + apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            System.out.println(apiKey);
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

            throw new RuntimeException("Failed to get response from Gemini");

        } catch (Exception e) {
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;  // No specific options needed
    }
}

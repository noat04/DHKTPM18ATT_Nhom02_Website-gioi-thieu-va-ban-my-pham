package org.fit.shopnuochoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration.class
})
@EnableScheduling // <-- BẮT BUỘC PHẢI CÓ
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

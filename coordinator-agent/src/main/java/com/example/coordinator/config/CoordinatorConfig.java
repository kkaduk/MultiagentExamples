// src/main/java/com/example/coordinator/CoordinatorConfig.java
package com.example.coordinator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CoordinatorConfig {

    @Value("${spring.ai.openai.api-key:demo}")
    private String openAiApiKey;

    // @Bean
    // public ChatClient chatClient() {
    //     return new OpenAiChatClient(new OpenAiApi(openAiApiKey));
    // }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}
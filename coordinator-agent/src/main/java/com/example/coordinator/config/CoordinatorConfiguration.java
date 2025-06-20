// package com.example.coordinator.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.openai.OpenAiChatModel;
// import net.kaduk.a2a.A2AWebClientService;
// import net.kaduk.a2a.A2AAgent;

// import java.util.HashMap;
// import java.util.Map;

// @Configuration
// public class CoordinatorConfiguration {

//     @Bean
//     public ChatClient chatClient(OpenAiChatModel chatModel) {
//         return ChatClient.builder(chatModel).build();
//     }

//     @Bean
//     public A2AWebClientService a2aWebClientService() {
//         return new A2AWebClientService();
//     }

//     @Bean
//     public Map<String, A2AAgent> agentClients() {
//         // Initialize with empty map - agents will register themselves
//         return new HashMap<>();
//     }
// }
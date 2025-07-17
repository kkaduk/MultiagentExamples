// src/main/java/com/example/coordinator/CoordinatorApplication.java
package com.example.coordinator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.example.coordinator.conversation.ConversationMemoryChatMemory;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.coordinator",
    "io.a2a.receptionist" // <--- Include this explicitly
})
public class CoordinatorApplication {
	public static void main(String[] args) {
		SpringApplication.run(CoordinatorApplication.class, args);
	}

	@Bean
	public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
			ConversationMemoryChatMemory memory) {

		var openAiOptions = OpenAiChatOptions.builder()
				.model("gpt-4o-mini")
				.temperature(0.4)
				.build();
		// AnthropicChatOptions anthropicOptions = AnthropicChatOptions.builder()
		// .model("claude-3-7-sonnet-latest")
		// .build();

		return chatClientBuilder
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.defaultAdvisors(new SimpleLoggerAdvisor())
				.defaultToolCallbacks(tools)
				.defaultSystem("Take a deep breath and work on this step by step.")
				.defaultOptions(openAiOptions)
				.build();
	}

	@Bean
	public ConversationMemoryChatMemory conversationMemory() {
		return new ConversationMemoryChatMemory();
	}

	// @Bean
	// public Receptionist receptionist(AgentRepositoryImpl agentRepository,
	// 		A2AWebClientService webClientService,
	// 		com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
	// 	return new Receptionist(agentRepository, webClientService, objectMapper);
	// }

	// @Bean
	// @ConditionalOnMissingBean
	// public A2AWebClientService a2aWebClientService(WebClient webClient) {
	// 	return new A2AWebClientService(webClient);
	// }

	// @Bean
	// @ConditionalOnMissingBean
	// @DependsOn("agentRepository")
	// public AgentRegistry a2aAgentRegistry(AgentRepository agentRepository) {
	// 	return new AgentRegistry(agentRepository);
	// }

}
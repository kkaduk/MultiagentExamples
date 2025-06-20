package com.oracle.mcp.client.kb.chat.conversation.advisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Advisor that rewrites the user query to be more concise and easier to
 * understand.
 *
 * <p>
 * This advisor intercepts the user query, applies a rewriting template, and
 * replaces the original query with the rewritten version. The original and
 * rewritten queries are stored in the advice context.
 * </p>
 *
 * @author
 * @since 1.0.0
 */

public class QuestionRewriteAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    public static final String ORIGINAL_QUERY = "original_query";
    public static final String REWRITTEN_QUERY = "rewritten_query";

    //FIXME: The agents are hardcoded in the template. This should be dynamic.
    //It will be better to use the agent card to get the skills and agents.
    private static final String DEFAULT_QUERY_REWRITE_TEMPLATE = """
           Rewrite the following prompt to make it more [simple | formal | technical | optimized for ChatGPT | clear | specific]:
            {user_query}
            """;

    private final String queryRewriteTemplate;
    private final boolean protectFromBlocking;
    private final int order;

    @Value("${spring.ai.anthropic.api-key}")
    private String athKey;
    // private static ChatClient chatClient;

    /**
     * Creates a QueryRewriteAdvisor with the default rewriting template.
     */
    public QuestionRewriteAdvisor() {
        this(DEFAULT_QUERY_REWRITE_TEMPLATE, true, 0);
    }

    /**
     * Creates a QueryRewriteAdvisor with the specified rewriting template.
     *
     * @param queryRewriteTemplate the template used to rewrite the query
     * @param protectFromBlocking  if true the advisor will protect execution from
     *                             blocking threads
     * @param order                the order of the advisor
     */
    public QuestionRewriteAdvisor(String queryRewriteTemplate, boolean protectFromBlocking,
            int order) {
        Assert.hasText(queryRewriteTemplate, "The queryRewriteTemplate must not be empty!");
        this.queryRewriteTemplate = queryRewriteTemplate;
        this.protectFromBlocking = protectFromBlocking;
        this.order = order;
    }

    /**
     * Creates a builder for a QueryRewriteAdvisor.
     *
     * @return the builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        AdvisedRequest advisedRequestModified = before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequestModified);
        return after(advisedResponse);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(@NonNull AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        Flux<AdvisedResponse> advisedResponses = (this.protectFromBlocking)
                ? Mono.just(advisedRequest)
                        .publishOn(Schedulers.boundedElastic())
                        .map(this::before)
                        .flatMapMany(chain::nextAroundStream)
                : chain.nextAroundStream(before(advisedRequest));

        return advisedResponses.map(this::after);
    }

    private AdvisedRequest before(AdvisedRequest request) {
        Map<String, Object> context = new HashMap<>(request.adviseContext());

        // Capture the original query.
        String originalQuery = request.userText();
        context.put(ORIGINAL_QUERY, originalQuery);

        // Rewrite the query using the rewriting template.
        // This uses a PromptTemplate that replaces the {user_query} placeholder.
        String templetedQuery = new PromptTemplate(this.queryRewriteTemplate, Map.of("user_query", originalQuery))
                .render();
        context.put(REWRITTEN_QUERY, templetedQuery);

        var claudeRewrite = chatClaude(templetedQuery);

        // Replace the original user text with the message to worker.
        return AdvisedRequest.from(request)
                .userText(claudeRewrite)
                .adviseContext(context)
                .build();
    }

    private AdvisedResponse after(AdvisedResponse advisedResponse) {
        // Optionally, you can add rewriting metadata to the response.
        return advisedResponse;
    }

    /**
     * Builder for QueryRewriteAdvisor.
     */
    public static final class Builder {
        private String queryRewriteTemplate = DEFAULT_QUERY_REWRITE_TEMPLATE;
        private boolean protectFromBlocking = true;
        private int order = 0;

        private Builder() {
        }

        /**
         * Sets a custom rewriting template.
         *
         * @param queryRewriteTemplate the template to use for rewriting
         * @return this builder instance
         */
        public Builder queryRewriteTemplate(String queryRewriteTemplate) {
            Assert.hasText(queryRewriteTemplate, "The queryRewriteTemplate must not be empty!");
            this.queryRewriteTemplate = queryRewriteTemplate;
            return this;
        }

        /**
         * Sets whether to protect from blocking threads.
         *
         * @param protectFromBlocking true if protection is desired, false otherwise
         * @return this builder instance
         */
        public Builder protectFromBlocking(boolean protectFromBlocking) {
            this.protectFromBlocking = protectFromBlocking;
            return this;
        }

        /**
         * Sets the order of this advisor.
         *
         * @param order the advisor order
         * @return this builder instance
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * Builds a QueryRewriteAdvisor instance.
         *
         * @return a new QueryRewriteAdvisor
         */
        public QuestionRewriteAdvisor build() {
            return new QuestionRewriteAdvisor(queryRewriteTemplate, protectFromBlocking, order);
        }
    }

    private String chatLlama(String prompt) {

        OllamaApi ollamaApi = new OllamaApi("http://homemainstation:11434");

        OllamaChatModel chatModel = OllamaChatModel.builder()
                // .ollamaApi(new OllamaApi())
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaOptions.builder()
                                .internalToolExecutionEnabled(true)
                                .model("qwq:latest")
                                .topK(99)
                                .temperature(66.6)
                                .numGPU(0)
                                .build())
                .build();

        // Message message = MessageBuilder.createMessage(payload, messageHeaders);
        var resp = chatModel.call(prompt);
        return resp;

    }

    private String chatClaude(String prompt) {

        AnthropicApi api = new AnthropicApi(athKey);
        var options = AnthropicChatOptions.builder()
                .model(AnthropicApi.ChatModel.CLAUDE_3_HAIKU.getValue())
                .maxTokens(2048)
                .stopSequences(List.of("this-is-the-end"))
                .temperature(0.7)
                .topK(1)
                .topP(1.0)
                .httpHeaders(Map.of("x-api-key", System.getenv("ANTHROPIC_API_KEY")))
                // .model("claude-3-5-sonnet-20240620")
                .build();
        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(options)
                .build();

        Prompt prm = new Prompt(prompt, options);

        // List<String> collection = ChatClient.create(chatModel).prompt()
        // .user(u -> u.text("List five {subject}")
        // .param("subject", "ice cream flavors"))
        // .call()
        // .entity(new ParameterizedTypeReference<>() {
        // });

        var resp = chatModel.call(prm);

        return resp.getResult().getOutput().getText();

    }
}

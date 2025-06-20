package com.example.coordinator.conversation.advisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;

import com.example.coordinator.model.WorkerSkilsDTO;
import static com.example.coordinator.tools.TaskPromptGenerator.generatePrompt;

import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CoordynatorAgentAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    public static final String ORIGINAL_QUERY = "original_query";
    public static final String REWRITTEN_QUERY = "rewritten_query";

    private final boolean protectFromBlocking;
    private final int order;

    public CoordynatorAgentAdvisor(boolean protectFromBlocking, int order) {
        this.protectFromBlocking = protectFromBlocking;
        this.order = order;
    }

    public CoordynatorAgentAdvisor() {
        this(true, 0);
    }
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
        List<WorkerSkilsDTO> workers = Receptionist.getRegisteredWorkerSkils();

        String taskDistributionPrompt = generatePrompt(originalQuery, workers);
        context.put(REWRITTEN_QUERY, taskDistributionPrompt);
        return AdvisedRequest.from(request)
                .userText(taskDistributionPrompt)
                .adviseContext(context)
                .build();
    }

    private AdvisedResponse after(AdvisedResponse advisedResponse) {
        // Optionally, you can add rewriting metadata to the response.
        return advisedResponse;
    }

    /**
     * Builder for CoordynatorAgentAdvisor.
     */
    public static final class Builder {
        private boolean protectFromBlocking = true;
        private int order = 0;

        private Builder() {
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
         * Builds a CoordynatorAgentAdvisor instance.
         *
         * @return a new CoordynatorAgentAdvisor
         */
        public CoordynatorAgentAdvisor build() {
            return new CoordynatorAgentAdvisor(protectFromBlocking, order);
        }
    }
}

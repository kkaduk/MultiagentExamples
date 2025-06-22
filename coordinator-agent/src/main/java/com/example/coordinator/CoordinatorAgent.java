// src/main/java/com/example/coordinator/CoordinatorAgent.java
package com.example.coordinator;

import net.kaduk.a2a.*;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@A2AAgent(name = "TaskCoordinator", version = "1.0.0", description = "Coordinates complex tasks by delegating to specialized worker agents", url = "http://localhost:8081")
public class CoordinatorAgent {

    private final ChatClient chatClient;
    private final WebClient webClient;
    private final Map<String, WorkerAgent> availableWorkers = new ConcurrentHashMap<>();
    private final Map<String, TaskExecution> runningTasks = new ConcurrentHashMap<>();

    @Autowired
    Receptionist receptionist;

    // List of known worker endpoints to discover
    private final List<String> workerEndpoints = Arrays.asList(
            "http://localhost:8082",
            "http://localhost:8083"
    // Add more worker endpoints as needed
    );

    public CoordinatorAgent(ChatClient chatClient, WebClient webClient) {
        this.chatClient = chatClient;
        this.webClient = webClient;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeWorkersOnStartup() {
        System.out.println("🔍 Discovering available workers...");
        discoverWorkers();
    }

    private void discoverWorkers() {
        for (String endpoint : workerEndpoints) {
            try {

                // External capability query to discover workers
                CapabilityQuery query = CapabilityQuery.builder()
                        .requiredTags(Arrays.asList("nlp", "text-processing"))
                        .keywords(Arrays.asList("analyze", "sentiment"))
                        .maxResults(5)
                        .build();
                CapabilityQuery queryAll = CapabilityQuery.builder()
                        .requiredTags(Arrays.asList("nlp", "text-processing"))
                        .keywords(Arrays.asList("analyze", "sentiment"))
                        .maxResults(5)
                        .build();
                CapabilityDiscoveryResponse skils = webClient.post()
                        .uri(endpoint + "/a2a/receptionist/discover")
                        .bodyValue(queryAll)
                        .retrieve()
                        .bodyToMono(CapabilityDiscoveryResponse.class)
                        .timeout(java.time.Duration.ofSeconds(10))
                        .block();
                // Internal capability query to discover workers
                Mono<List<AgentCapabilityInfo>> skills = receptionist.findAgentsByCapability(queryAll);

                skills.subscribe(agentCapabilities -> {
                    System.out.println("Discovered agent capabilities: " + agentCapabilities);
                });

                System.out.println("Attempting to discover worker at: " + endpoint);

                // Fetch agent card from worker
                AgentCard agentCard = webClient.get()
                        .uri(endpoint + "/agent/card")
                        .retrieve()
                        .bodyToMono(AgentCard.class)
                        .timeout(java.time.Duration.ofSeconds(10))
                        .block();

                if (agentCard != null) {
                    WorkerAgent worker = createWorkerFromAgentCard(agentCard, endpoint);
                    String workerId = generateWorkerId(worker);
                    availableWorkers.put(workerId, worker);

                    System.out.println("✅ Discovered worker: " + worker.getName() +
                            " with capabilities: " + worker.getCapabilities());
                } else {
                    System.out.println("❌ No agent card received from: " + endpoint);
                }

            } catch (Exception e) {
                System.out.println("❌ Failed to discover worker at " + endpoint + ": " + e.getMessage());
            }
        }

        System.out.println("🎯 Discovery complete. Found " + availableWorkers.size() + " workers");
        printWorkerSummary();
    }

    private WorkerAgent createWorkerFromAgentCard(AgentCard agentCard, String endpoint) {
        // Extract capabilities from skills
        List<String> capabilities = new ArrayList<>();

        if (agentCard.getSkills() != null) {
            for (AgentSkill skill : agentCard.getSkills()) {
                // Add skill tags as capabilities
                if (skill.getTags() != null) {
                    capabilities.addAll(skill.getTags());
                }

                // Add skill names as capabilities (normalized)
                if (skill.getName() != null) {
                    capabilities.add(skill.getName().toLowerCase().replace(" ", "-"));
                }
            }
        }

        // Remove duplicates and normalize
        capabilities = capabilities.stream()
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());

        return new WorkerAgent(
                agentCard.getName(),
                endpoint,
                capabilities,
                agentCard.getSkills(),
                agentCard.getDescription());
    }

    private String generateWorkerId(WorkerAgent worker) {
        // Generate a consistent ID based on worker characteristics
        return worker.getName().toLowerCase().replace(" ", "-");
    }

    private void printWorkerSummary() {
        System.out.println("\n📋 Available Workers Summary:");
        System.out.println("=".repeat(60));

        for (Map.Entry<String, WorkerAgent> entry : availableWorkers.entrySet()) {
            WorkerAgent worker = entry.getValue();
            System.out.println("🤖 " + worker.getName() + " (" + entry.getKey() + ")");
            System.out.println("   📍 URL: " + worker.getUrl());
            System.out.println("   🏷️  Capabilities: " + worker.getCapabilities());
            System.out.println("   ⚡ Skills: " +
                    worker.getSkills().stream()
                            .map(AgentSkill::getName)
                            .collect(Collectors.joining(", ")));
            System.out.println();
        }
    }

    @A2AAgentSkill(id = "coordinate-task", name = "Coordinate Complex Task", description = "Coordinates complex tasks by planning, delegating, and aggregating results", tags = {
            "coordination", "planning", "management" })
    public String coordinateTask(String userQuery) {
        try {
            System.out.println("Starting coordination for: " + userQuery);

            // Step 1: Plan the task using LLM with discovered worker capabilities
            TaskPlan plan = planTaskWithDiscoveredWorkers(userQuery);
            System.out.println("Task plan created with " + plan.getSubtasks().size() + " subtasks");

            // Step 2: Execute subtasks and collect results
            List<SubtaskResult> results = new ArrayList<>();

            for (SubTask subtask : plan.getSubtasks()) {
                WorkerAgent worker = findBestWorkerForTask(subtask);
                if (worker != null) {
                    System.out.println("Executing subtask " + subtask.getId() + " on worker " + worker.getName());

                    try {
                        SubtaskResult result = executeSubtaskSync(worker, subtask);
                        results.add(result);
                        System.out.println("Subtask " + subtask.getId() + " completed: " + result.isSuccess());
                    } catch (Exception e) {
                        System.err.println("Error executing subtask " + subtask.getId() + ": " + e.getMessage());
                        results.add(new SubtaskResult(subtask.getId(), false, null, e.getMessage()));
                    }
                } else {
                    System.err.println("No suitable worker found for subtask: " + subtask.getId());
                    results.add(new SubtaskResult(subtask.getId(), false, null, "No suitable worker found"));
                }
            }

            // Step 3: Aggregate results
            String aggregatedResult = aggregateResults(userQuery, results);
            System.out.println("Task coordination completed");

            return aggregatedResult;

        } catch (Exception e) {
            System.err.println("Error in coordinateTask: " + e.getMessage());
            e.printStackTrace();
            return "Error coordinating task: " + e.getMessage();
        }
    }

    private TaskPlan planTaskWithDiscoveredWorkers(String userQuery) {
        // Create a detailed description of available workers for the LLM
        StringBuilder workerDescription = new StringBuilder();
        workerDescription.append("Available workers and their capabilities:\n");

        for (Map.Entry<String, WorkerAgent> entry : availableWorkers.entrySet()) {
            WorkerAgent worker = entry.getValue();
            workerDescription.append(String.format(
                    "%d. %s (ID: %s)\n   - Description: %s\n   - Capabilities: %s\n   - Skills: %s\n\n",
                    workerDescription.toString().split("\n").length / 4,
                    worker.getName(),
                    entry.getKey(),
                    worker.getDescription(),
                    String.join(", ", worker.getCapabilities()),
                    worker.getSkills().stream()
                            .map(skill -> skill.getName() + " (" + skill.getId() + ")")
                            .collect(Collectors.joining(", "))));
        }

        String planningPrompt = """
                You are a task planning AI. Given a user query, break it down into subtasks that can be executed by the available specialized workers.

                %s

                User Query: %s

                Analyze the query and determine which workers can handle different aspects. Consider:
                1. Data processing tasks (numerical analysis, calculations, trends)
                2. Text processing tasks (sentiment analysis, NLP, summarization)
                3. Other specialized capabilities

                Please respond with a simple breakdown of which worker should handle which part of the task.
                Format: WorkerID: Task description
                """
                .formatted(workerDescription.toString(), userQuery);

        try {
            Prompt prompt = new Prompt(new UserMessage(planningPrompt));

            CallResponseSpec response = chatClient.prompt(prompt).call();
            var planResponse = response.content();

            return parseTaskPlanFromResponse(planResponse, userQuery);
        } catch (Exception e) {
            System.err.println("Error planning task with LLM: " + e.getMessage());
            return createIntelligentFallbackPlan(userQuery);
        }
    }

    private TaskPlan parseTaskPlanFromResponse(String planResponse, String userQuery) {
        TaskPlan plan = new TaskPlan();
        List<SubTask> subtasks = new ArrayList<>();

        // Extract data and text components from the original query
        String dataComponent = extractDataComponent(userQuery);
        String textComponent = extractTextComponent(userQuery);

        // Find best workers for each component type
        if (!dataComponent.isEmpty()) {
            WorkerAgent dataWorker = findWorkerByCapability(
                    Arrays.asList("data-processing", "analysis", "computation", "mathematics"));
            if (dataWorker != null) {
                subtasks.add(new SubTask("data_task", "Process data components",
                        generateWorkerId(dataWorker), dataComponent, findBestSkillId(dataWorker, dataComponent)));
            }
        }

        if (!textComponent.isEmpty()) {
            WorkerAgent textWorker = findWorkerByCapability(
                    Arrays.asList("text-processing", "nlp", "language", "sentiment"));
            if (textWorker != null) {
                subtasks.add(new SubTask("text_task", "Process text components",
                        generateWorkerId(textWorker), textComponent, findBestSkillId(textWorker, textComponent)));
            }
        }

        if (subtasks.isEmpty()) {
            // If no specific components found, try to find any available worker
            if (!availableWorkers.isEmpty()) {
                WorkerAgent anyWorker = availableWorkers.values().iterator().next();
                subtasks.add(new SubTask("default_task", "Process user query",
                        generateWorkerId(anyWorker), userQuery, findBestSkillId(anyWorker, userQuery)));
            }
        }

        plan.setSubtasks(subtasks);
        return plan;
    }

    private WorkerAgent findWorkerByCapability(List<String> requiredCapabilities) {
        for (WorkerAgent worker : availableWorkers.values()) {
            for (String capability : requiredCapabilities) {
                if (worker.getCapabilities().contains(capability)) {
                    return worker;
                }
            }
        }
        return null;
    }

    private String findBestSkillId(WorkerAgent worker, String input) {
        // Find the most appropriate skill for the given input
        for (AgentSkill skill : worker.getSkills()) {
            if (skill.getTags() != null) {
                for (String tag : skill.getTags()) {
                    if (input.toLowerCase().contains(tag.toLowerCase()) ||
                            skill.getDescription().toLowerCase().contains("process")) {
                        return skill.getId();
                    }
                }
            }
        }

        // Return first available skill as fallback
        return worker.getSkills().isEmpty() ? "default" : worker.getSkills().get(0).getId();
    }

    private TaskPlan createIntelligentFallbackPlan(String userQuery) {
        TaskPlan plan = new TaskPlan();
        List<SubTask> subtasks = new ArrayList<>();

        String dataComponent = extractDataComponent(userQuery);
        String textComponent = extractTextComponent(userQuery);

        if (!dataComponent.isEmpty()) {
            WorkerAgent dataWorker = findWorkerByCapability(
                    Arrays.asList("data-processing", "analysis", "computation"));
            if (dataWorker != null) {
                subtasks.add(new SubTask("data_task", "Process data aspects",
                        generateWorkerId(dataWorker), dataComponent, findBestSkillId(dataWorker, dataComponent)));
            }
        }

        if (!textComponent.isEmpty()) {
            WorkerAgent textWorker = findWorkerByCapability(Arrays.asList("text-processing", "nlp", "language"));
            if (textWorker != null) {
                subtasks.add(new SubTask("text_task", "Process text aspects",
                        generateWorkerId(textWorker), textComponent, findBestSkillId(textWorker, textComponent)));
            }
        }

        if (subtasks.isEmpty() && !availableWorkers.isEmpty()) {
            WorkerAgent anyWorker = availableWorkers.values().iterator().next();
            subtasks.add(new SubTask("default_task", "Process user query",
                    generateWorkerId(anyWorker), userQuery, findBestSkillId(anyWorker, userQuery)));
        }

        plan.setSubtasks(subtasks);
        return plan;
    }

    private WorkerAgent findBestWorkerForTask(SubTask subtask) {
        return availableWorkers.get(subtask.getWorkerType());
    }

    private SubtaskResult executeSubtaskSync(WorkerAgent worker, SubTask subtask) throws Exception {
        try {
            SendMessageRequest request = createA2ARequest(subtask);

            System.out.println("Sending request to " + worker.getUrl() + "/agent/message");
            System.out.println("Using skill: " + subtask.getSkillId());

            SendMessageSuccessResponse response = webClient.post()
                    .uri(worker.getUrl() + "/agent/message")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SendMessageSuccessResponse.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            if (response != null && response.getResult() != null) {
                String result = extractTextFromMessage(response.getResult());
                System.out.println("Received response from " + worker.getName() + ": " +
                        result.substring(0, Math.min(100, result.length())) + "...");
                return new SubtaskResult(subtask.getId(), true, result, null);
            } else {
                return new SubtaskResult(subtask.getId(), false, null, "No response from worker");
            }

        } catch (Exception e) {
            System.err.println("Error executing subtask " + subtask.getId() + ": " + e.getMessage());
            throw e;
        }
    }

    private SendMessageRequest createA2ARequest(SubTask subtask) {
        Message message = Message.builder()
                .kind("message")
                .messageId(UUID.randomUUID().toString())
                .role("user")
                .taskId(subtask.getSkillId()) // Use the discovered skill ID
                .contextId(UUID.randomUUID().toString())
                .parts(Collections.singletonList(TextPart.builder()
                        .text(subtask.getInput())
                        .build()))
                .build();

        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(MessageSendConfiguration.builder()
                        .acceptedOutputModes(Collections.singletonList("text/plain"))
                        .blocking(true)
                        .build())
                .build();

        return SendMessageRequest.builder()
                .id(UUID.randomUUID().toString())
                .params(params)
                .build();
    }

    // Additional method to refresh worker discovery
    @A2AAgentSkill(id = "refresh-workers", name = "Refresh Worker Discovery", description = "Rediscovers available workers and their capabilities", tags = {
            "discovery", "refresh", "management" })
    public String refreshWorkers() {
        availableWorkers.clear();
        discoverWorkers();
        return "Worker discovery refreshed. Found " + availableWorkers.size() + " workers.";
    }

    // Method to list discovered workers
    @A2AAgentSkill(id = "list-workers", name = "List Available Workers", description = "Lists all discovered workers and their capabilities", tags = {
            "list", "workers", "capabilities" })
    public String listWorkers() {
        if (availableWorkers.isEmpty()) {
            return "No workers currently discovered. Try running refresh-workers first.";
        }

        StringBuilder result = new StringBuilder("🤖 Discovered Workers:\n\n");

        for (Map.Entry<String, WorkerAgent> entry : availableWorkers.entrySet()) {
            WorkerAgent worker = entry.getValue();
            result.append("📋 ").append(worker.getName()).append(" (").append(entry.getKey()).append(")\n");
            result.append("   📍 URL: ").append(worker.getUrl()).append("\n");
            result.append("   📝 Description: ").append(worker.getDescription()).append("\n");
            result.append("   🏷️  Capabilities: ").append(String.join(", ", worker.getCapabilities())).append("\n");
            result.append("   ⚡ Skills:\n");

            for (AgentSkill skill : worker.getSkills()) {
                result.append("      - ").append(skill.getName())
                        .append(" (").append(skill.getId()).append(")")
                        .append(" - ").append(skill.getDescription()).append("\n");
            }
            result.append("\n");
        }

        return result.toString();
    }

    // Keep existing helper methods for data/text extraction and result
    // aggregation...
    private String extractDataComponent(String userQuery) {
        StringBuilder dataComponent = new StringBuilder();

        if (userQuery.toLowerCase().contains("data") || userQuery.toLowerCase().contains("numbers")) {
            String[] words = userQuery.split("\\s+");
            List<String> numbers = new ArrayList<>();

            for (String word : words) {
                String cleanWord = word.replaceAll("[^0-9.]", "");
                if (cleanWord.matches("\\d+(\\.\\d+)?")) {
                    numbers.add(cleanWord);
                }
            }

            if (!numbers.isEmpty()) {
                dataComponent.append("Analyze these numbers: ");
                dataComponent.append(String.join(", ", numbers));
                dataComponent.append(". Original context: ").append(userQuery);
            }
        }

        return dataComponent.toString();
    }

    private String extractTextComponent(String userQuery) {
        if (userQuery.toLowerCase().contains("sentiment") ||
                userQuery.toLowerCase().contains("text") ||
                userQuery.toLowerCase().contains("customer") ||
                userQuery.contains("\"") || userQuery.contains("'")) {

            StringBuilder textComponent = new StringBuilder();

            if (userQuery.contains("\"")) {
                int start = userQuery.indexOf("\"");
                int end = userQuery.lastIndexOf("\"");
                if (start != end && start != -1 && end != -1) {
                    textComponent.append(userQuery.substring(start + 1, end));
                }
            } else if (userQuery.contains(":")) {
                String[] parts = userQuery.split(":");
                if (parts.length > 1) {
                    String afterColon = parts[parts.length - 1].trim();
                    if (afterColon.length() > 10) {
                        textComponent.append(afterColon);
                    }
                }
            }

            if (textComponent.length() == 0) {
                textComponent.append(userQuery);
            }
        }

        return "";
    }

    private String extractTextFromMessage(Message message) {
        if (message.getParts() != null && !message.getParts().isEmpty()) {
            Part firstPart = message.getParts().get(0);
            if (firstPart instanceof TextPart) {
                return ((TextPart) firstPart).getText();
            }
        }
        return "No text content found";
    }

    private String aggregateResults(String originalQuery, List<SubtaskResult> results) {
        StringBuilder aggregatedResult = new StringBuilder();
        aggregatedResult.append("🎯 Task Coordination Results\n");
        aggregatedResult.append("=".repeat(50)).append("\n\n");
        aggregatedResult.append("📋 Original Query: ").append(originalQuery).append("\n\n");

        List<SubtaskResult> successfulResults = new ArrayList<>();
        List<SubtaskResult> failedResults = new ArrayList<>();

        for (SubtaskResult result : results) {
            if (result.isSuccess()) {
                successfulResults.add(result);
            } else {
                failedResults.add(result);
            }
        }

        if (!successfulResults.isEmpty()) {
            aggregatedResult.append("✅ Successfully Completed Tasks:\n");
            aggregatedResult.append("-".repeat(40)).append("\n\n");

            for (SubtaskResult result : successfulResults) {
                aggregatedResult.append("🔸 ").append(getTaskName(result.getSubtaskId())).append(":\n");
                aggregatedResult.append(result.getResult()).append("\n\n");
            }
        }

        if (!failedResults.isEmpty()) {
            aggregatedResult.append("❌ Failed Tasks:\n");
            aggregatedResult.append("-".repeat(40)).append("\n\n");

            for (SubtaskResult result : failedResults) {
                aggregatedResult.append("🔸 ").append(getTaskName(result.getSubtaskId())).append(":\n");
                aggregatedResult.append("Error: ").append(result.getError()).append("\n\n");
            }
        }

        if (!successfulResults.isEmpty()) {
            aggregatedResult.append("📊 Executive Summary:\n");
            aggregatedResult.append("-".repeat(40)).append("\n");
            aggregatedResult.append(createFinalSummary(originalQuery, successfulResults));
        }

        return aggregatedResult.toString();
    }

    private String getTaskName(String taskId) {
        switch (taskId) {
            case "data_task":
                return "Data Analysis Task";
            case "text_task":
                return "Text Processing Task";
            default:
                return "Task " + taskId;
        }
    }

    private String createFinalSummary(String originalQuery, List<SubtaskResult> successfulResults) {
        StringBuilder resultsContext = new StringBuilder();
        for (SubtaskResult result : successfulResults) {
            resultsContext.append(result.getResult()).append("\n\n");
        }

        String summaryPrompt = """
                Please create a concise executive summary that integrates the following task execution results to answer the original user query:

                Original Query: %s

                Task Results:
                %s

                Provide a coherent summary that directly addresses the user's question by combining insights from all completed tasks.
                """
                .formatted(originalQuery, resultsContext.toString());

        try {

            Prompt prompt = new Prompt(new UserMessage(summaryPrompt));
            CallResponseSpec response = chatClient.prompt(prompt).call();

            return response.content();

        } catch (Exception e) {
            System.err.println("Error creating final summary: " + e.getMessage());
            return "Task coordination completed successfully. Please review the individual task results above.";
        }
    }

    // Updated supporting classes
    public static class TaskPlan {
        private List<SubTask> subtasks;

        public List<SubTask> getSubtasks() {
            return subtasks;
        }

        public void setSubtasks(List<SubTask> subtasks) {
            this.subtasks = subtasks;
        }
    }

    public static class SubTask {
        private String id;
        private String description;
        private String workerType;
        private String input;
        private String skillId;

        public SubTask(String id, String description, String workerType, String input, String skillId) {
            this.id = id;
            this.description = description;
            this.workerType = workerType;
            this.input = input;
            this.skillId = skillId;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String getWorkerType() {
            return workerType;
        }

        public String getInput() {
            return input;
        }

        public String getSkillId() {
            return skillId;
        }
    }

    public static class SubtaskResult {
        private String subtaskId;
        private boolean success;
        private String result;
        private String error;

        public SubtaskResult(String subtaskId, boolean success, String result, String error) {
            this.subtaskId = subtaskId;
            this.success = success;
            this.result = result;
            this.error = error;
        }

        public String getSubtaskId() {
            return subtaskId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getResult() {
            return result;
        }

        public String getError() {
            return error;
        }
    }

    public static class WorkerAgent {
        private String name;
        private String url;
        private List<String> capabilities;
        private List<AgentSkill> skills;
        private String description;

        public WorkerAgent(String name, String url, List<String> capabilities, List<AgentSkill> skills,
                String description) {
            this.name = name;
            this.url = url;
            this.capabilities = capabilities;
            this.skills = skills != null ? skills : new ArrayList<>();
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public List<String> getCapabilities() {
            return capabilities;
        }

        public List<AgentSkill> getSkills() {
            return skills;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class TaskExecution {
        private String taskId;
        private String status;

        public TaskExecution(String taskId, String status) {
            this.taskId = taskId;
            this.status = status;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
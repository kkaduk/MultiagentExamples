package com.example.coordinator.workflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.kaduk.a2a.A2AAgent;
import net.kaduk.a2a.A2AWebClientService;
import net.kaduk.a2a.Message;
import net.kaduk.a2a.MessageSendConfiguration;
import net.kaduk.a2a.MessageSendParams;
import net.kaduk.a2a.Part;
import net.kaduk.a2a.SendMessageRequest;
import net.kaduk.a2a.SendMessageSuccessResponse;
import net.kaduk.a2a.TextPart;

@Service
public class WorkflowOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowOrchestrator.class);

    private final ObjectMapper objectMapper;
    private final Map<String, A2AAgent> agentClients;
    private final A2AWebClientService a2aClient;

    // Store for tasks and their results
    private final Map<Integer, WorkflowTask> workflowTasks = new ConcurrentHashMap<>();
    private final Map<String, Integer> agentToTaskMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> taskResults = new ConcurrentHashMap<>();

    // Workflow execution state
    private final AtomicBoolean workflowRunning = new AtomicBoolean(false);
    private CountDownLatch workflowCompletionLatch;

    public WorkflowOrchestrator(ObjectMapper objectMapper, 
                               Map<String, A2AAgent> agentClients,
                               A2AWebClientService a2aClient) {
        this.objectMapper = objectMapper;
        this.agentClients = agentClients;
        this.a2aClient = a2aClient;
    }

    /**
     * Start the workflow execution based on the provided plan
     */
    public CompletableFuture<Map<Integer, String>> executeWorkflow(List<WorkflowTask> tasks) {
        // Reset workflow state
        resetWorkflowState();

        // Initialize workflow state
        tasks.forEach(task -> workflowTasks.put(task.getTaskNumber(), task));
        workflowRunning.set(true);
        workflowCompletionLatch = new CountDownLatch(tasks.size());

        // Find tasks that can be started immediately (no predecessors)
        List<WorkflowTask> readyTasks = tasks.stream()
                .filter(task -> task.getRequiredPredecessor() == null)
                .collect(Collectors.toList());

        // Start the ready tasks
        readyTasks.forEach(this::executeTask);

        // Return a future that will complete when the workflow is done
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Wait for workflow completion (with timeout for safety)
                boolean completed = workflowCompletionLatch.await(30, TimeUnit.MINUTES);
                if (!completed) {
                    throw new RuntimeException("Workflow execution timed out after 30 minutes");
                }
                return taskResults;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Workflow execution was interrupted", e);
            } finally {
                workflowRunning.set(false);
            }
        });
    }

    /**
     * Reset the workflow state
     */
    private void resetWorkflowState() {
        workflowTasks.clear();
        agentToTaskMap.clear();
        taskResults.clear();
        workflowRunning.set(false);
    }

    /**
     * Execute a specific task
     */
    private void executeTask(WorkflowTask task) {
        logger.info("Executing task #{}: {} using agent: {}",
                task.getTaskNumber(), task.getAssignedSubtask(), task.getAgent());

        updateTaskStatus(task.getTaskNumber(), "RUNNING");

        // Map agent to task for callback handling
        agentToTaskMap.put(task.getAgent(), task.getTaskNumber());

        // Build the message including any results from predecessors
        String taskMessage = buildTaskMessage(task);

        // Get agent URL - you'll need to map agent names to URLs
        String agentUrl = getAgentUrl(task.getAgent());
        
        if (agentUrl == null) {
            handleTaskFailure(task, new RuntimeException("No URL found for agent: " + task.getAgent()));
            return;
        }

        // Create the A2A message request
        SendMessageRequest request = SendMessageRequest.builder()
                .id(java.util.UUID.randomUUID().toString())
                .params(MessageSendParams.builder()
                        .message(Message.builder()
                                .kind("message")
                                .messageId(java.util.UUID.randomUUID().toString())
                                .role("user")
                                .taskId(task.getAgent().toLowerCase())
                                .contextId(String.valueOf(task.getTaskNumber()))
                                .parts(java.util.Collections.singletonList(TextPart.builder()
                                        .text(taskMessage)
                                        .build()))
                                .build())
                        .configuration(MessageSendConfiguration.builder()
                                .acceptedOutputModes(java.util.Collections.singletonList("text/plain"))
                                .blocking(true)
                                .build())
                        .build())
                .build();

        // Send the task to the agent
        try {
            a2aClient.sendMessage(agentUrl, request)
                    .doOnSuccess(response -> handleTaskSuccess(task, response))
                    .doOnError(error -> handleTaskFailure(task, error))
                    .subscribe();
        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
    }

    /**
     * Handle successful task completion
     */
    private void handleTaskSuccess(WorkflowTask task, SendMessageSuccessResponse response) {
        logger.info("Task #{} completed successfully", task.getTaskNumber());
        
        String result = extractResultFromResponse(response);
        taskResults.put(task.getTaskNumber(), result);
        updateTaskStatus(task.getTaskNumber(), "COMPLETED");
        
        // Find and trigger dependent tasks
        triggerDependentTasks(task.getTaskNumber());
        
        // Mark this task as done
        workflowCompletionLatch.countDown();
    }

    /**
     * Build the task message, including any predecessor results
     */
    private String buildTaskMessage(WorkflowTask task) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(task.getAssignedSubtask());

        // Append previous results if there are any
        if (task.getRequiredPredecessor() != null) {
            String predecessorResult = taskResults.get(task.getRequiredPredecessor());
            if (predecessorResult != null && !predecessorResult.isEmpty()) {
                messageBuilder.append("\n\nPrevious task result:\n");
                messageBuilder.append(predecessorResult);
            }
        }

        return messageBuilder.toString();
    }

    /**
     * Handle task failures
     */
    private void handleTaskFailure(WorkflowTask task, Throwable error) {
        logger.error("Task #{} failed: {}", task.getTaskNumber(), error.getMessage(), error);
        updateTaskStatus(task.getTaskNumber(), "FAILED");
        taskResults.put(task.getTaskNumber(), "FAILED: " + error.getMessage());
        workflowCompletionLatch.countDown();
    }

    /**
     * Update task status
     */
    private void updateTaskStatus(int taskNumber, String status) {
        WorkflowTask task = workflowTasks.get(taskNumber);
        if (task != null) {
            task.setStatus(status);
            workflowTasks.put(taskNumber, task);
        }
    }

    /**
     * Trigger dependent tasks when a task completes
     */
    private void triggerDependentTasks(int completedTaskNumber) {
        // Find tasks that depend on the completed task
        List<WorkflowTask> dependentTasks = workflowTasks.values().stream()
                .filter(t -> Objects.equals(t.getRequiredPredecessor(), completedTaskNumber))
                .filter(t -> !"RUNNING".equals(t.getStatus()) && !"COMPLETED".equals(t.getStatus()))
                .collect(Collectors.toList());

        // Execute each dependent task
        dependentTasks.forEach(this::executeTask);
    }

    /**
     * Extract result from A2A response
     */
    private String extractResultFromResponse(SendMessageSuccessResponse response) {
        if (response != null && response.getResult() != null) {
            Message resultMessage = response.getResult();
            if (resultMessage.getParts() != null && !resultMessage.getParts().isEmpty()) {
                Part firstPart = resultMessage.getParts().get(0);
                if (firstPart instanceof TextPart) {
                    return ((TextPart) firstPart).getText();
                }
            }
        }
        return "No result received";
    }

    /**
     * Get agent URL by name - you should implement this based on your agent registry
     */
    private String getAgentUrl(String agentName) {
        // This is a simple mapping - in a real system you'd use a service registry
        Map<String, String> agentUrls = Map.of(
            "DataProcessor", "http://localhost:8081/agent/message",
            "MLTrainer", "http://localhost:8082/agent/message",
            "DeploymentAgent", "http://localhost:8083/agent/message"
        );
        
        return agentUrls.get(agentName);
    }
}
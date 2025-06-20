package com.example.coordinator.workflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
// import com.oracle.a2a.client.A2AClient;
// import com.oracle.a2a.model.Message;
// import com.oracle.a2a.model.Part;
// import com.oracle.a2a.model.TaskArtifactUpdateEvent;
// import com.oracle.a2a.model.TaskSendParams;
// import com.oracle.a2a.model.TaskState;
// import com.oracle.a2a.model.TaskStatusUpdateEvent;
// import com.oracle.a2a.model.TextPart;
import net.kaduk.a2a.A2AAgent;

@Service
public class WorkflowOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowOrchestrator.class);

    private final ObjectMapper objectMapper;
    private final Map<String, A2AAgent> agentClients;

    // Store for tasks and their results
    private final Map<Integer, WorkflowTask> workflowTasks = new ConcurrentHashMap<>();
    private final Map<String, Integer> agentToTaskMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> taskResults = new ConcurrentHashMap<>();

    // Workflow execution state
    private final AtomicBoolean workflowRunning = new AtomicBoolean(false);
    private CountDownLatch workflowCompletionLatch;

    public WorkflowOrchestrator(ObjectMapper objectMapper, Map<String, A2AAgent> agentClients) {
        this.objectMapper = objectMapper;
        this.agentClients = agentClients;
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

        // Find the A2A client for this agent
        A2AClient client = agentClients.getOrDefault(
                task.getAgent().toLowerCase().replaceAll("\\s+", ""),
                agentClients.get("default"));

        if (client == null) {
            handleTaskFailure(task, new RuntimeException("No A2A client found for agent: " + task.getAgent()));
            return;
        }

        // Map agent to task for callback handling
        agentToTaskMap.put(task.getAgent(), task.getTaskNumber());

        // Build the message including any results from predecessors
        String taskMessage = buildTaskMessage(task);

        // Create task parameters with the message
        TaskSendParams params = new TaskSendParams();
        Message message = new Message();
        TextPart textPart = new TextPart();
        textPart.setText(taskMessage);
        message.setParts(new Part[] { textPart });
        params.setMessage(message);

        // Send the task to the agent with streaming updates
        try {
            client.sendAndSubscribe(
                    params,
                    this::handleStatusUpdate,
                    this::handleArtifactUpdate,
                    error -> handleTaskFailure(task, error))
                    .exceptionally(ex -> {
                        handleTaskFailure(task, ex);
                        return null;
                    });
        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
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
     * Handle task status updates
     */
    private void handleStatusUpdate(TaskStatusUpdateEvent event) {
        logger.debug("Received status update for task: {}, status: {}",
                event.getId(), event.getStatus());

        if (event.getStatus().getState() != null && TaskState.fromValue(event.getStatus().getState().toString()) == TaskState.COMPLETED) {
            // Use content from the task, not getResponse()
            String result = event.getStatus().getMessage() != null ? event.getStatus().getMessage().toString()
                    : "No content";
            logger.info("Task completed with result: {}", result);

            // Find the task associated with this update
            Optional<Map.Entry<String, Integer>> taskEntry = agentToTaskMap.entrySet().stream()
                    .filter(entry -> event.getId().contains(entry.getKey()))
                    .findFirst();

            if (taskEntry.isPresent()) {
                Integer taskNumber = taskEntry.get().getValue(); // Fixed: use get() instead of getValue()
                WorkflowTask task = workflowTasks.get(taskNumber);

                // Store the result
                taskResults.put(taskNumber, result);
                updateTaskStatus(taskNumber, "COMPLETED");

                // Find and trigger dependent tasks
                triggerDependentTasks(taskNumber);

                // Mark this task as done
                workflowCompletionLatch.countDown();
            } else {
                logger.warn("Received status update for unknown task: {}", event.getId());
            }
        } else if ("failed".equals(event.getStatus())) { //FIXIT NOT String comparision
            // Get the error message
            String errorMessage = event.getStatus().getMessage() != null ? event.getStatus().getMessage().toString()
                    : "Unknown error";
            logger.error("Task failed: {}", errorMessage);

            // Find and fail dependent tasks as well
            Optional<Map.Entry<String, Integer>> taskEntry = agentToTaskMap.entrySet().stream()
                    .filter(entry -> event.getId().contains(entry.getKey()))
                    .findFirst();

            if (taskEntry.isPresent()) {
                Integer taskNumber = taskEntry.get().getValue(); // Fixed: use get() instead of getValue()
                updateTaskStatus(taskNumber, "FAILED");
                workflowCompletionLatch.countDown();
            }
        }
    }

    /**
     * Handle task artifact updates
     */
    private void handleArtifactUpdate(TaskArtifactUpdateEvent event) {
        logger.debug("Received artifact update: {}", event.getArtifact().getDescription());
        // Here you could store or process artifacts if needed
    }

    /**
     * Handle task failures
     */
    private void handleTaskFailure(WorkflowTask task, Throwable error) {
        logger.error("Task #{} failed: {}", task.getTaskNumber(), error.getMessage(), error);
        updateTaskStatus(task.getTaskNumber(), "FAILED");
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
                .collect(Collectors.toList());

        // Execute each dependent task
        dependentTasks.forEach(this::executeTask);
    }
}
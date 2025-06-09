// src/main/java/com/example/coordinator/TaskDispatcher.java
package com.example.coordinator;

import com.example.coordinator.model.AggregatedResult;
import com.example.coordinator.model.WorkerTask;
import net.kaduk.a2a.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskDispatcher {

    private final A2AWebClientService a2aClient;

    // Constructor injection with @Lazy to avoid circular dependency
    public TaskDispatcher(@Lazy A2AWebClientService a2aClient) {
        this.a2aClient = a2aClient;
    }

    private final Map<String, WorkerTask> activeTasks = new ConcurrentHashMap<>();
    
    private static final String WORKER_A_URL = "http://localhost:8081/agent/message";
    private static final String WORKER_B_URL = "http://localhost:8082/agent/message";

    public Mono<AggregatedResult> dispatchAndAggregateWork(String input, String coordinationId) {
        long startTime = System.currentTimeMillis();
        
        System.out.println("Starting coordination for input: " + input);
        
        // Create tasks for both workers
        WorkerTask taskA = createWorkerTask("worker-a", input, "transform", coordinationId);
        WorkerTask taskB = createWorkerTask("worker-b", input, "analyze", coordinationId);
        
        activeTasks.put(taskA.getTaskId(), taskA);
        activeTasks.put(taskB.getTaskId(), taskB);

        // Dispatch to Worker A (Text transformation)
        Mono<SendMessageSuccessResponse> workerAResponse = sendTaskToWorker(WORKER_A_URL, taskA);
        
        // Dispatch to Worker B (Text analysis)
        Mono<SendMessageSuccessResponse> workerBResponse = sendTaskToWorker(WORKER_B_URL, taskB);

        // Wait for both workers to complete and aggregate results
        return Mono.zip(workerAResponse, workerBResponse)
                .map(tuple -> {
                    SendMessageSuccessResponse responseA = tuple.getT1();
                    SendMessageSuccessResponse responseB = tuple.getT2();
                    
                    System.out.println("Received responses from both workers");
                    
                    // Extract results from responses
                    String resultA = extractResultFromResponse(responseA);
                    String resultB = extractResultFromResponse(responseB);
                    
                    System.out.println("Worker A result: " + resultA);
                    System.out.println("Worker B result: " + resultB);
                    
                    // Update task results
                    taskA.setResult(resultA);
                    taskA.setStatus("completed");
                    taskB.setResult(resultB);
                    taskB.setStatus("completed");
                    
                    // Remove from active tasks
                    activeTasks.remove(taskA.getTaskId());
                    activeTasks.remove(taskB.getTaskId());
                    
                    // Create aggregated result
                    return AggregatedResult.builder()
                            .coordinationId(coordinationId)
                            .completedTasks(Arrays.asList(taskA, taskB))
                            .aggregatedData(Map.of(
                                    "originalInput", input,
                                    "transformedText", resultA,
                                    "analysisResult", resultB,
                                    "combinedLength", resultA.length() + resultB.length(),
                                    "workerAProcessingTime", taskA.getTimestamp(),
                                    "workerBProcessingTime", taskB.getTimestamp()
                            ))
                            .summary(String.format(
                                    "Successfully coordinated processing of '%s'. Transformation: '%s', Analysis: '%s'", 
                                    input, resultA, resultB))
                            .totalProcessingTime(System.currentTimeMillis() - startTime)
                            .status("completed")
                            .build();
                })
                .timeout(Duration.ofSeconds(30))
                .doOnError(error -> {
                    System.err.println("Coordination failed: " + error.getMessage());
                    // Clean up active tasks on error
                    activeTasks.remove(taskA.getTaskId());
                    activeTasks.remove(taskB.getTaskId());
                })
                .onErrorReturn(AggregatedResult.builder()
                        .coordinationId(coordinationId)
                        .status("failed")
                        .summary("Failed to complete worker tasks: coordination timeout or error")
                        .totalProcessingTime(System.currentTimeMillis() - startTime)
                        .build());
    }

    private WorkerTask createWorkerTask(String workerId, String input, String skillId, String coordinationId) {
        return WorkerTask.builder()
                .taskId(UUID.randomUUID().toString())
                .workerId(workerId)
                .input(input)
                .skillId(skillId)
                .status("dispatched")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private Mono<SendMessageSuccessResponse> sendTaskToWorker(String workerUrl, WorkerTask task) {
        System.out.println("Sending task to worker: " + workerUrl + " with skill: " + task.getSkillId());
        
        SendMessageRequest request = SendMessageRequest.builder()
                .id(UUID.randomUUID().toString())
                .params(MessageSendParams.builder()
                        .message(Message.builder()
                                .kind("message")
                                .messageId(UUID.randomUUID().toString())
                                .role("user")
                                .taskId(task.getSkillId())
                                .contextId(task.getTaskId())
                                .parts(Collections.singletonList(TextPart.builder()
                                        .text(task.getInput())
                                        .build()))
                                .build())
                        .configuration(MessageSendConfiguration.builder()
                                .acceptedOutputModes(Collections.singletonList("text/plain"))
                                .blocking(true)
                                .build())
                        .build())
                .build();

        return a2aClient.sendMessage(workerUrl, request)
                .doOnSuccess(response -> {
                    task.setStatus("completed");
                    System.out.println("Task " + task.getTaskId() + " completed for worker " + task.getWorkerId());
                })
                .doOnError(error -> {
                    task.setStatus("failed");
                    System.err.println("Task " + task.getTaskId() + " failed for worker " + task.getWorkerId() + ": " + error.getMessage());
                });
    }

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

    public Map<String, WorkerTask> getActiveTasks() {
        return new HashMap<>(activeTasks);
    }
}
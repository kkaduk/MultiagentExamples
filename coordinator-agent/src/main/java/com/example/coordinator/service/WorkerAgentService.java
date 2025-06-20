package com.example.coordinator.service;

import java.util.UUID;

import net.kaduk.a2a.A2AAgent;
import net.kaduk.a2a.Message;
import net.kaduk.a2a.Part;
import net.kaduk.a2a.Task;
import net.kaduk.a2a.TaskIdParams;
import net.kaduk.a2a.TaskQueryParams;
import net.kaduk.a2a.TaskState;
import net.kaduk.a2a.TextPart;

// import com.oracle.a2a.client.A2AClient;
// import com.oracle.a2a.model.Message;
// import com.oracle.a2a.model.Part;
// import com.oracle.a2a.model.Task;
// import com.oracle.a2a.model.TaskSendParams;
// import com.oracle.a2a.model.TaskState;
// import com.oracle.a2a.model.TextPart;

public class WorkerAgentService {

    private String getWorkerResponse(String prompt, A2AAgent client) {
        // Use TaskSendParams instead of TaskIdParams to set both ID and Message
        TaskQueryParams params = new TaskQueryParams();
        params.setId(UUID.randomUUID().toString()); // Generate a unique task ID

        Message message = new Message();
        message.setRole("user");

        TextPart textPart = new TextPart();
        textPart.setText("Hello! Can you help me with some information?");

        message.setParts(java.util.Collections.singletonList(textPart));
        params.setMessage(message);

        // Example 1: Send a synchronous request
        System.out.println("\n=== Synchronous Request ===");
        Task response = client.sendMessage(params);
        System.out.println("Task ID: " + response.getId());
        System.out.println("State: " + response.getStatus().getState());

        // If the task is State is working, loop until it is completed
        // If the task is State is working, loop until it is completed
        while (response.getStatus().getState().equals(TaskState.WORKING)) {
            System.out.println("Task is still working...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } // Wait for 1 second before checking again
            response = client.getTask(response.getId(), 1);
        }
        System.out.println("Task completed with state: " + response.getStatus().getState());
        // System.out.println("Task result: " +
        // response.getArtifacts()[0].getDescription());

        // If the task is completed, print the agent's response
        String agentResponse = "Hello! Can you help me with some information?";
        if (response.getStatus().getMessage() != null) {
            Part responsePart = response.getStatus().getMessage().getParts()[0];
            if (responsePart instanceof TextPart) {
                System.out.println("Agent response: " + ((TextPart) responsePart).getText());
                agentResponse = ((TextPart) responsePart).getText();
            }
        }
        return agentResponse;
    }

}

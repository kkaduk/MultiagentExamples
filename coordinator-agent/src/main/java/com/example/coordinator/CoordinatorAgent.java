package com.example.coordinator;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.coordinator.conversation.advisor.Receptionist;
import com.example.coordinator.model.WorkerSkilsDTO;
import com.example.coordinator.workflow.WorkflowOrchestrator;
import com.example.coordinator.workflow.WorkflowTask;

import net.kaduk.a2a.A2AAgent;
import net.kaduk.a2a.A2AAgentSkill;

@A2AAgent(
    name = "CoordinatorAgent", 
    version = "1.0", 
    description = "Coordinates tasks between multiple worker agents using acyclic graph workflow",
    url = "http://localhost:8080"
)
@Component
public class CoordinatorAgent {

    @Autowired
    private WorkflowOrchestrator workflowOrchestrator;
    
    @Autowired(required = false)
    private ChatClient chatClient;

    @A2AAgentSkill(
        id = "coordinate", 
        name = "Coordinate Multi-Agent Task", 
        description = "Dispatches work to multiple agents using acyclic graph workflow",
        tags = {"coordination", "orchestration", "multi-agent", "workflow"}
    )
    public String coordinate(String input) {
        try {
            // Get available agent skills
            List<WorkerSkilsDTO> availableAgents = Receptionist.getRegisteredWorkerSkils();
            
            if (availableAgents.isEmpty()) {
                return "No worker agents available for coordination.";
            }
            
            // For now, create a simple workflow without AI planning
            // You can enhance this later when ChatClient is properly configured
            List<WorkflowTask> workflowTasks = createSimpleWorkflow(input, availableAgents);
            
            // Execute the workflow
            var result = workflowOrchestrator.executeWorkflow(workflowTasks).get();
            
            return String.format(
                "Coordination completed. Tasks executed: %d. Results: %s",
                workflowTasks.size(),
                result.toString()
            );
            
        } catch (Exception e) {
            return "Coordination failed: " + e.getMessage();
        }
    }

    @A2AAgentSkill(
        id = "list-agents", 
        name = "List Available Agents", 
        description = "Returns a list of all available worker agents and their skills",
        tags = {"agents", "skills", "discovery"}
    )
    public String listAvailableAgents() {
        List<WorkerSkilsDTO> agents = Receptionist.getRegisteredWorkerSkils();
        
        if (agents.isEmpty()) {
            return "No worker agents are currently registered.";
        }
        
        StringBuilder result = new StringBuilder("Available Worker Agents:\n");
        for (WorkerSkilsDTO agent : agents) {
            result.append(String.format("- %s (%s): %s\n", 
                agent.getName(), 
                agent.getServerUrl(),
                String.join(", ", agent.getSkills())
            ));
        }
        
        return result.toString();
    }

    @A2AAgentSkill(
        id = "ping", 
        name = "Ping", 
        description = "Simple health check",
        tags = {"health", "ping"}
    )
    public String ping(String message) {
        return "Coordinator pong: " + (message != null ? message : "");
    }
    
    private List<WorkflowTask> createSimpleWorkflow(String input, List<WorkerSkilsDTO> agents) {
        // Create a simple sequential workflow
        List<WorkflowTask> tasks = new java.util.ArrayList<>();
        
        for (int i = 0; i < agents.size(); i++) {
            WorkerSkilsDTO agent = agents.get(i);
            WorkflowTask task = new WorkflowTask();
            task.setTaskNumber(i + 1);
            task.setAgent(agent.getName());
            task.setAssignedSubtask("Process: " + input + " using skills: " + String.join(", ", agent.getSkills()));
            task.setReason("Selected based on available skills");
            task.setRequiredPredecessor(i == 0 ? null : i); // Sequential execution
            task.setStatus("PENDING");
            
            tasks.add(task);
        }
        
        return tasks;
    }
}
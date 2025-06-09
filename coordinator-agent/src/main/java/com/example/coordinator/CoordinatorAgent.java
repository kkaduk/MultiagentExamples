// src/main/java/com/example/coordinator/CoordinatorAgent.java
package com.example.coordinator;

import com.example.coordinator.model.AggregatedResult;
import net.kaduk.a2a.A2AAgent;
import net.kaduk.a2a.A2AAgentSkill;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@A2AAgent(
    name = "CoordinatorAgent", 
    version = "1.0", 
    description = "Coordinates tasks between multiple worker agents"
)
@Component
public class CoordinatorAgent {

    private final TaskDispatcher taskDispatcher;

    // Constructor injection with @Lazy to avoid circular dependency
    public CoordinatorAgent(@Lazy TaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
    }

    @A2AAgentSkill(
        id = "coordinate", 
        name = "Coordinate Multi-Agent Task", 
        description = "Dispatches work to multiple agents and aggregates results",
        tags = {"coordination", "orchestration", "multi-agent"}
    )
    public String coordinate(String input) {
        String coordinationId = UUID.randomUUID().toString();
        
        System.out.println("Coordinator received request: " + input);
        
        try {
            // Block for synchronous response (in real scenarios, consider async handling)
            AggregatedResult result = taskDispatcher.dispatchAndAggregateWork(input, coordinationId).block();
            
            if (result != null && "completed".equals(result.getStatus())) {
                return String.format(
                    "Coordination completed in %dms. Summary: %s",
                    result.getTotalProcessingTime(),
                    result.getSummary()
                );
            } else {
                return "Coordination failed: " + (result != null ? result.getSummary() : "Unknown error");
            }
        } catch (Exception e) {
            System.err.println("Coordination error: " + e.getMessage());
            return "Coordination failed with error: " + e.getMessage();
        }
    }

    @A2AAgentSkill(
        id = "status", 
        name = "Get Task Status", 
        description = "Returns the status of active coordination tasks",
        tags = {"status", "monitoring"}
    )
    public String getStatus() {
        int activeTasks = taskDispatcher.getActiveTasks().size();
        return String.format("Coordinator active with %d tasks in progress", activeTasks);
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
}
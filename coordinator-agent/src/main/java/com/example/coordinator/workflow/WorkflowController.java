package com.example.coordinator.workflow;


// import com.oracle.a2a.service.WorkflowOrchestrator;
// import com.oracle.a2a.workflow.WorkflowTask;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    
    private final WorkflowOrchestrator orchestrator;
    
    public WorkflowController(WorkflowOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
    

    //It is example, the logic will be used in chat loop, just after user request query response
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<Map<Integer, String>>> executeWorkflow(
            @RequestBody List<WorkflowTask> tasks) {
        return orchestrator.executeWorkflow(tasks)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body(Map.of(0, "Workflow execution failed: " + ex.getMessage())));
    }
}
package com.example.coordinator.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTask {
    private int taskNumber;
    private String agent;
    private String assignedSubtask;
    private String reason;
    private Integer requiredPredecessor;
    private String result;
    private String status = "PENDING";
}
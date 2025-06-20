package com.example.coordinator.model

import lombok.Data;

@Data
public class TaskAssignmentDTO {
    private String agent;
    private String assigned_subtask;
    private String reason;

    public TaskAssignmentDTO() {}

    public TaskAssignmentDTO(String agent, String assigned_subtask, String reason) {
        this.agent = agent;
        this.assigned_subtask = assigned_subtask;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "TaskAssignment{" +
                "agent='" + agent + '\'' +
                ", assigned_subtask='" + assigned_subtask + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}

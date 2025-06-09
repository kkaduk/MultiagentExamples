package com.example.coordinator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkerTask {
    private String taskId;
    private String workerId;
    private String input;
    private String skillId;
    private String status;
    private String result;
    private long timestamp;
}
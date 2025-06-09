package com.example.coordinator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AggregatedResult {
    private String coordinationId;
    private List<WorkerTask> completedTasks;
    private Map<String, Object> aggregatedData;
    private String summary;
    private long totalProcessingTime;
    private String status;
}
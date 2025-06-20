package com.example.coordinator.model;

import java.util.List;

import lombok.Data;

@Data
public class WorkerSkilsDTO {
    private final String name;
    private final String serverUrl;
    private final List<String> skills;
}

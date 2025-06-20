package com.example.coordinator.conversation.advisor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.example.coordinator.model.WorkerSkilsDTO;

@Component
public class Receptionist {
    
    private static final ConcurrentMap<String, WorkerSkilsDTO> registeredWorkers = new ConcurrentHashMap<>();
    
    static {
        // Initialize with some default workers - these should be dynamically registered
        registerWorker(new WorkerSkilsDTO("DataProcessor", "http://localhost:8081", 
            Arrays.asList("data preprocessing", "feature engineering", "data analysis")));
        registerWorker(new WorkerSkilsDTO("MLTrainer", "http://localhost:8082", 
            Arrays.asList("model training", "hyperparameter tuning", "model evaluation")));
        registerWorker(new WorkerSkilsDTO("DeploymentAgent", "http://localhost:8083", 
            Arrays.asList("deployment", "CI/CD", "monitoring", "containerization")));
    }
    
    public static void registerWorker(WorkerSkilsDTO worker) {
        registeredWorkers.put(worker.getName(), worker);
    }
    
    public static void unregisterWorker(String workerName) {
        registeredWorkers.remove(workerName);
    }
    
    public static List<WorkerSkilsDTO> getRegisteredWorkerSkils() {
        return new ArrayList<>(registeredWorkers.values());
    }
    
    public static WorkerSkilsDTO getWorkerByName(String name) {
        return registeredWorkers.get(name);
    }
}
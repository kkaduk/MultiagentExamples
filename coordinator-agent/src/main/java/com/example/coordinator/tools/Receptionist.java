package com.example.coordinator.tools;

import java.util.Arrays;
import java.util.List;

import com.example.coordinator.model.WorkerSkilsDTO;

import net.kaduk.a2a.A2AAgent;
import net.kaduk.a2a.AgentCard;

// import com.oracle.a2a.client.A2AClient;
// import com.oracle.a2a.model.AgentCard;
// import com.oracle.mcp.client.kb.chat.model.WorkerSkilsDTO;

public class Receptionist {

    public List<AgentCard> getRegisteredWorkerList() {
        return null;
    }

    public static List<WorkerSkilsDTO> getRegisteredWorkerSkils() {

        // Iterate over all registered workers
        // Replace with the correct way to obtain an A2AAgent instance, e.g., using a
        // factory or a concrete subclass
        // A2AAgent client = A2AAgent.create("http://localhost:8090/a2a/api");
        // AgentCard card = client.getAgentCard();
        // String skills = card.getSkills().toString();

        // Placeholder only - For test
        List<WorkerSkilsDTO> agents = Arrays.asList(
                new WorkerSkilsDTO("financtial report", "http://localhost:8090/a2a/api",
                        List.of("generate yearly financial report")),
                new WorkerSkilsDTO("financtial data sources", "http://localhost:8090/a2a/api",
                        List.of("access financial data")),
                new WorkerSkilsDTO("email sender", "http://localhost:8090/a2a/api",
                        List.of("create and send email to reciepiens")),
                new WorkerSkilsDTO("hotel booking agent", "http://localhost:8090/a2a/api",
                        List.of("book hotel room", "cancel booking")));
        return agents;
    }

    public void registerWorker(AgentCard card) {
        // Crerate JPA entry with particular agent's skills
    }

    public void deregisterWorker(AgentCard card) {
        // Delete JPA entry with particular agent's skills
    }

    public void updateWorker(AgentCard card) {
        // Update JPA entry with particular agent's skills
    }
}

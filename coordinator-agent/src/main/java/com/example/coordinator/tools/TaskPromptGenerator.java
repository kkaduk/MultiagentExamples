package com.example.coordinator.tools;

import java.util.List;

import com.example.coordinator.model.WorkerSkilsDTO;

// import com.oracle.mcp.client.kb.chat.model.WorkerSkilsDTO;

public class TaskPromptGenerator {



    public static String generatePrompt(String userQuery, List<WorkerSkilsDTO> agents) {
        StringBuilder agentDescriptions = new StringBuilder();
        for (WorkerSkilsDTO worker : agents) {
            String skillsFormatted = String.join("\", \"", worker.getSkills());
            agentDescriptions.append(String.format("-  %s: skills = [\"%s\"]\n", worker.getName(), skillsFormatted));
        }

        return String.format("""
                You are a task distribution planner in a multi-agent system.
                Task: %s
                Agents:
                %s
                Distribute the task into subtasks based on agent skills and plan the required sequence of tasks by defining task predecessors.
                Omit agents whose skills are unsuitable for the required task from the prepared workflow. 
                Return output as JSON with fields: "task namber", "agent", "assigned_subtask", "reason", "required predecessor".
                """, userQuery, agentDescriptions.toString().trim());
    }
}

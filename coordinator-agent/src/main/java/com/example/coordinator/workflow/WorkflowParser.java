package com.example.coordinator.workflow;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WorkflowParser {
    public static List<WorkflowTask> parseTasks(String rawJson) {
        try {
            // Clean Markdown-style backticks and optional prefix like "ASSISTANT:"
            String cleaned = rawJson
                    .replaceAll("(?i)^ASSISTANT:\\s*", "") // remove prefix
                    .replaceAll("(?m)^```json\\s*", "") // remove ```json
                    .replaceAll("(?m)^```\\s*$", "") // remove closing ```
                    .trim();

            ObjectMapper mapper = new ObjectMapper();

            // Read as JsonNode to remap fields
            JsonNode root = mapper.readTree(cleaned);

            for (JsonNode node : root) {
                ((ObjectNode) node).set("taskNumber", node.get("task number"));
                ((ObjectNode) node).set("assignedSubtask", node.get("assigned_subtask"));
                ((ObjectNode) node).set("requiredPredecessor", node.get("required predecessor"));
            }

            return mapper.readerFor(new TypeReference<List<WorkflowTask>>() {
            }).readValue(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse workflow tasks", e);
        }
    }
}

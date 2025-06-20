package com.example.coordinator.customizer;

import java.time.Duration;
import java.util.List;

import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult.StopReason;
import io.modelcontextprotocol.spec.McpSchema.Root;

@Component
public class CustomMcpSyncClientCustomizer implements McpSyncClientCustomizer {
    @Override
    public void customize(String serverConfigurationName, McpClient.SyncSpec spec) {

        // Customize the request configuration
        spec.requestTimeout(Duration.ofSeconds(60));

        // Instantiate using the record’s generated constructor
        Root rootInstance = new Root("file:///Users/KKADUK/Project/Ontology/mcpclient", "My File Resource");

        // Sets the root URIs that the server connecto this client can access.
        spec.roots(rootInstance); // Could be LIS as well

        // Sets a custom sampling handler for processing message creation requests.
        spec.sampling(request -> {

            var meta = request.messages();

            
            // Construct the result (not a Mono)
            CreateMessageResult result = CreateMessageResult.builder()
                    .message("serverConfigurationName")
                    .role(McpSchema.Role.ASSISTANT)
                    // .content(Content)
                    .model("claude-3-sonnet-20240307")
                    .stopReason(StopReason.END_TURN)
                    .build();
            return result;
        });

        // Adds a consumer to be notified when the available tools change, such as tools
        // being added or removed.
        spec.toolsChangeConsumer((List<McpSchema.Tool> tools) -> {
            System.out.println("Tools changed: " + tools.get(0).name());
        });

        // Adds a consumer to be notified when the available resources change, such as
        // resources
        // being added or removed.
        spec.resourcesChangeConsumer((List<McpSchema.Resource> resources) -> {
            System.out.println("Resource changed: " + resources.get(0).name());
        });

        // Adds a consumer to be notified when the available prompts change, such as
        // prompts
        // being added or removed.
        spec.promptsChangeConsumer((List<McpSchema.Prompt> prompts) -> {
            // Handle prompts change
        });

        // Adds a consumer to be notified when logging messages are received from the
        // server.
        spec.loggingConsumer((McpSchema.LoggingMessageNotification log) -> {
            System.out.println("Liging changed: " + log);
        });
    }
}
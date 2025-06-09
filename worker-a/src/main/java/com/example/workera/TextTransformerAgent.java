package com.example.workera;

import com.example.workera.service.TextTransformationService;
import net.kaduk.a2a.A2AAgent;
import net.kaduk.a2a.A2AAgentSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@A2AAgent(
    name = "TextTransformerAgent", 
    version = "1.0", 
    description = "Performs various text transformation operations"
)
@Component
public class TextTransformerAgent {

    @Autowired
    private TextTransformationService transformationService;

    @A2AAgentSkill(
        id = "transform", 
        name = "Transform Text", 
        description = "Transforms input text using multiple operations (uppercase, reverse words, format)",
        tags = {"text", "transformation", "processing"}
    )
    public String transform(String input) {
        System.out.println("Worker A received transformation request: " + input);
        String result = transformationService.transform(input);
        System.out.println("Worker A transformation result: " + result);
        return result;
    }

    @A2AAgentSkill(
        id = "reverse", 
        name = "Reverse Text", 
        description = "Reverses the input text character by character",
        tags = {"text", "reverse", "processing"}
    )
    public String reverse(String input) {
        System.out.println("Worker A received reverse request: " + input);
        String result = transformationService.reverseTransform(input);
        System.out.println("Worker A reverse result: " + result);
        return result;
    }

    @A2AAgentSkill(
        id = "keywords", 
        name = "Extract Keywords", 
        description = "Extracts keywords from input text",
        tags = {"text", "keywords", "extraction"}
    )
    public String extractKeywords(String input) {
        System.out.println("Worker A received keyword extraction request: " + input);
        String result = transformationService.extractKeywords(input);
        System.out.println("Worker A keyword extraction result: " + result);
        return result;
    }

    @A2AAgentSkill(
        id = "ping", 
        name = "Ping", 
        description = "Simple health check",
        tags = {"health", "ping"}
    )
    public String ping(String message) {
        return "Worker A pong: " + message;
    }
}
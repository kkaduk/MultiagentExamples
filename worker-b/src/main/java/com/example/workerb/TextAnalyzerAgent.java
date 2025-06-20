package com.example.workerb;

import com.example.workerb.service.TextAnalysisService;
import net.kaduk.a2a.A2AAgent;
import net.kaduk.a2a.A2AAgentSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@A2AAgent(
    name = "TextAnalyzerAgent", 
    version = "1.0", 
    description = "Performs comprehensive text analysis including statistics, sentiment, and readability",
    url = "http://localhost:8082"
)
@Component
public class TextAnalyzerAgent {

    @Autowired
    private TextAnalysisService analysisService;

    @A2AAgentSkill(
        id = "analyze", 
        name = "Analyze Text", 
        description = "Performs comprehensive text analysis including word count, sentiment, and readability",
        tags = {"text", "analysis", "statistics", "sentiment"}
    )
    public String analyze(String input) {
        System.out.println("Worker B received analysis request: " + input);
        String result = analysisService.analyze(input);
        System.out.println("Worker B analysis result: " + result);
        return result;
    }

    @A2AAgentSkill(
        id = "detailed-stats", 
        name = "Detailed Statistics", 
        description = "Provides detailed text statistics and analysis",
        tags = {"text", "statistics", "detailed", "analysis"}
    )
    public String getDetailedStats(String input) {
        System.out.println("Worker B received detailed stats request: " + input);
        String result = analysisService.getDetailedStats(input);
        System.out.println("Worker B detailed stats result: " + result);
        return result;
    }

    @A2AAgentSkill(
        id = "ping", 
        name = "Ping", 
        description = "Simple health check",
        tags = {"health", "ping"}
    )
    public String ping(String message) {
        return "Worker B pong: " + message;
    }
}
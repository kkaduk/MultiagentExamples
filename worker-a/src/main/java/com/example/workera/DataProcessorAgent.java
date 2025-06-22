// src/main/java/com/example/workera/DataProcessorAgent.java
package com.example.workera;

import net.kaduk.a2a.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@A2AAgent(
    name = "DataProcessor",
    version = "1.0.0",
    description = "Specialized agent for data processing, analysis, and computation tasks",
    url = "http://localhost:8082"
)
public class DataProcessorAgent {

    @A2AAgentSkill(
        id = "process-data",
        name = "Process Data",
        description = "Processes and analyzes numerical data, performs calculations, and generates insights",
        tags = {"data-processing", "analysis", "computation", "mathematics"}
    )
    public CompletableFuture<String> processData(String input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Extract numbers from input
                List<Double> numbers = extractNumbers(input);
                
                if (numbers.isEmpty()) {
                    return "No numerical data found to process. Please provide numerical data for analysis.";
                }
                
                // Perform various data analysis operations
                Map<String, Object> analysis = performDataAnalysis(numbers);
                
                // Generate insights
                String insights = generateInsights(analysis, input);
                
                return String.format("""
                    Data Processing Results:
                    
                    📊 Numerical Data Analysis:
                    - Dataset size: %d numbers
                    - Sum: %.2f
                    - Average: %.2f
                    - Minimum: %.2f
                    - Maximum: %.2f
                    - Standard Deviation: %.2f
                    
                    🔍 Insights:
                    %s
                    """, 
                    numbers.size(),
                    (Double) analysis.get("sum"),
                    (Double) analysis.get("average"),
                    (Double) analysis.get("min"),
                    (Double) analysis.get("max"),
                    (Double) analysis.get("stdDev"),
                    insights
                );
                
            } catch (Exception e) {
                return "Error processing data: " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "calculate",
        name = "Calculate",
        description = "Performs mathematical calculations and operations",
        tags = {"calculation", "mathematics", "arithmetic"}
    )
    public CompletableFuture<String> calculate(String expression) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simple calculator implementation
                double result = evaluateExpression(expression);
                return String.format("Calculation Result: %s = %.2f", expression, result);
            } catch (Exception e) {
                return "Error in calculation: " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "analyze-trends",
        name = "Analyze Trends",
        description = "Analyzes data trends and patterns",
        tags = {"trend-analysis", "patterns", "data-science"}
    )
    public CompletableFuture<String> analyzeTrends(String data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Double> numbers = extractNumbers(data);
                if (numbers.size() < 2) {
                    return "Insufficient data for trend analysis. Need at least 2 data points.";
                }
                
                String trend = determineTrend(numbers);
                double trendStrength = calculateTrendStrength(numbers);
                
                return String.format("""
                    📈 Trend Analysis Results:
                    
                    Trend Direction: %s
                    Trend Strength: %.2f
                    Data Points: %d
                    
                    Analysis: %s
                    """,
                    trend,
                    trendStrength,
                    numbers.size(),
                    generateTrendInsights(trend, trendStrength)
                );
                
            } catch (Exception e) {
                return "Error analyzing trends: " + e.getMessage();
            }
        });
    }

    // Helper methods
    private List<Double> extractNumbers(String input) {
        List<Double> numbers = new ArrayList<>();
        Pattern pattern = Pattern.compile("-?\\d+\\.?\\d*");
        Matcher matcher = pattern.matcher(input);
        
        while (matcher.find()) {
            try {
                numbers.add(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }
        
        return numbers;
    }

    private Map<String, Object> performDataAnalysis(List<Double> numbers) {
        Map<String, Object> analysis = new HashMap<>();
        
        double sum = numbers.stream().mapToDouble(Double::doubleValue).sum();
        double average = sum / numbers.size();
        double min = numbers.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = numbers.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        
        // Calculate standard deviation
        double variance = numbers.stream()
            .mapToDouble(num -> Math.pow(num - average, 2))
            .sum() / numbers.size();
        double stdDev = Math.sqrt(variance);
        
        analysis.put("sum", sum);
        analysis.put("average", average);
        analysis.put("min", min);
        analysis.put("max", max);
        analysis.put("stdDev", stdDev);
        
        return analysis;
    }

    private String generateInsights(Map<String, Object> analysis, String originalInput) {
        StringBuilder insights = new StringBuilder();
        
        double average = (Double) analysis.get("average");
        double stdDev = (Double) analysis.get("stdDev");
        double min = (Double) analysis.get("min");
        double max = (Double) analysis.get("max");
        
        if (stdDev < average * 0.1) {
            insights.append("- Data shows low variability (consistent values)\n");
        } else if (stdDev > average * 0.5) {
            insights.append("- Data shows high variability (diverse values)\n");
        }
        
        double range = max - min;
        if (range > average * 2) {
            insights.append("- Wide range of values detected\n");
        }
        
        if (originalInput.toLowerCase().contains("sales")) {
            insights.append("- Sales data analysis complete\n");
        } else if (originalInput.toLowerCase().contains("temperature")) {
            insights.append("- Temperature data analysis complete\n");
        }
        
        return insights.toString();
    }

    private double evaluateExpression(String expression) {
        // Simple expression evaluator (basic implementation)
        expression = expression.replaceAll("\\s+", "");
        
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }
        
        return Double.parseDouble(expression);
    }

    private String determineTrend(List<Double> numbers) {
        if (numbers.size() < 2) return "Unknown";
        
        int increasing = 0;
        int decreasing = 0;
        
        for (int i = 1; i < numbers.size(); i++) {
            if (numbers.get(i) > numbers.get(i-1)) {
                increasing++;
            } else if (numbers.get(i) < numbers.get(i-1)) {
                decreasing++;
            }
        }
        
        if (increasing > decreasing) {
            return "Increasing";
        } else if (decreasing > increasing) {
            return "Decreasing";
        } else {
            return "Stable";
        }
    }

    private double calculateTrendStrength(List<Double> numbers) {
        if (numbers.size() < 2) return 0.0;
        
        double totalChange = 0;
        for (int i = 1; i < numbers.size(); i++) {
            totalChange += Math.abs(numbers.get(i) - numbers.get(i-1));
        }
        
        return totalChange / (numbers.size() - 1);
    }

    private String generateTrendInsights(String trend, double strength) {
        StringBuilder insights = new StringBuilder();
        
        switch (trend) {
            case "Increasing":
                insights.append("The data shows an upward trend. ");
                break;
            case "Decreasing":
                insights.append("The data shows a downward trend. ");
                break;
            case "Stable":
                insights.append("The data remains relatively stable. ");
                break;
        }
        
        if (strength > 10) {
            insights.append("The trend is strong and consistent.");
        } else if (strength > 5) {
            insights.append("The trend is moderate.");
        } else {
            insights.append("The trend is weak or subtle.");
        }
        
        return insights.toString();
    }
}
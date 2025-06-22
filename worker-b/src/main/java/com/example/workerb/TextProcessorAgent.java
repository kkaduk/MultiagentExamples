// src/main/java/com/example/workerb/TextProcessorAgent.java
package com.example.workerb;

import net.kaduk.a2a.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@A2AAgent(
    name = "TextProcessor",
    version = "1.0.0",
    description = "Specialized agent for text processing, NLP, and language analysis tasks",
    url = "http://localhost:8083"
)
public class TextProcessorAgent {

    private static final Set<String> POSITIVE_WORDS = Set.of(
        "good", "great", "excellent", "amazing", "wonderful", "fantastic", "awesome", 
        "perfect", "brilliant", "outstanding", "superb", "magnificent", "love", "like"
    );
    
    private static final Set<String> NEGATIVE_WORDS = Set.of(
        "bad", "terrible", "awful", "horrible", "disgusting", "hate", "dislike", 
        "poor", "worst", "disappointing", "frustrating", "annoying", "useless"
    );

    @A2AAgentSkill(
        id = "process-text",
        name = "Process Text",
        description = "Processes and analyzes text content, extracts information, and provides insights",
        tags = {"text-processing", "nlp", "analysis", "language"}
    )
    public CompletableFuture<String> processText(String input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (input == null || input.trim().isEmpty()) {
                    return "No text provided for processing.";
                }
                
                // Perform comprehensive text analysis
                Map<String, Object> analysis = performTextAnalysis(input);
                
                return String.format("""
                    📝 Text Processing Results:
                    
                    📊 Basic Statistics:
                    - Character count: %d
                    - Word count: %d
                    - Sentence count: %d
                    - Paragraph count: %d
                    
                    🔤 Language Analysis:
                    - Average word length: %.1f characters
                    - Readability score: %s
                    - Complexity level: %s
                    
                    😊 Sentiment Analysis:
                    - Overall sentiment: %s
                    - Positive words: %d
                    - Negative words: %d
                    - Sentiment score: %.2f
                    
                    🔍 Key Insights:
                    %s
                    """,
                    (Integer) analysis.get("charCount"),
                    (Integer) analysis.get("wordCount"),
                    (Integer) analysis.get("sentenceCount"),
                    (Integer) analysis.get("paragraphCount"),
                    (Double) analysis.get("avgWordLength"),
                    analysis.get("readability"),
                    analysis.get("complexity"),
                    analysis.get("sentiment"),
                    (Integer) analysis.get("positiveWords"),
                    (Integer) analysis.get("negativeWords"),
                    (Double) analysis.get("sentimentScore"),
                    analysis.get("insights")
                );
                
            } catch (Exception e) {
                return "Error processing text: " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "analyze-sentiment",
        name = "Analyze Sentiment",
        description = "Analyzes the sentiment and emotional tone of text",
        tags = {"sentiment", "emotion", "analysis", "nlp"}
    )
    public CompletableFuture<String> analyzeSentiment(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SentimentResult sentiment = performSentimentAnalysis(text);
                
                return String.format("""
                    😊 Sentiment Analysis Results:
                    
                    Overall Sentiment: %s
                    Confidence Score: %.2f
                    
                    Breakdown:
                    - Positive indicators: %d
                    - Negative indicators: %d
                    - Neutral indicators: %d
                    
                    Analysis: %s
                    """,
                    sentiment.getSentiment(),
                    sentiment.getScore(),
                    sentiment.getPositiveCount(),
                    sentiment.getNegativeCount(),
                    sentiment.getNeutralCount(),
                    sentiment.getAnalysis()
                );
                
            } catch (Exception e) {
                return "Error analyzing sentiment: " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "extract-keywords",
        name = "Extract Keywords",
        description = "Extracts important keywords and phrases from text",
        tags = {"keywords", "extraction", "nlp", "information"}
    )
    public CompletableFuture<String> extractKeywords(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> keywords = performKeywordExtraction(text);
                
                return String.format("""
                    🔑 Keyword Extraction Results:
                    
                    Top Keywords:
                    %s
                    
                    Total keywords found: %d
                    """,
                    keywords.stream()
                        .limit(10)
                        .map(keyword -> "- " + keyword)
                        .collect(Collectors.joining("\n")),
                    keywords.size()
                );
                
            } catch (Exception e) {
                return "Error extracting keywords: " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "summarize-text",
        name = "Summarize Text",
        description = "Creates a concise summary of longer text content",
        tags = {"summarization", "nlp", "text-reduction"}
    )
    public CompletableFuture<String> summarizeText(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String summary = performTextSummarization(text);
                
                return String.format("""
                    📋 Text Summarization Results:
                    
                    Original length: %d characters
                    Summary length: %d characters
                    Compression ratio: %.1f%%
                    
                    Summary:
                    %s
                    """,
                    text.length(),
                    summary.length(),
                    (1.0 - (double) summary.length() / text.length()) * 100,
                    summary
                );
                
            } catch (Exception e) {
                return "Error summarizing text: " + e.getMessage();
            }
        });
    }

    // Helper methods
    private Map<String, Object> performTextAnalysis(String text) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Basic statistics
        analysis.put("charCount", text.length());
        
        String[] words = text.toLowerCase().split("\\s+");
        analysis.put("wordCount", words.length);
        
        String[] sentences = text.split("[.!?]+");
        analysis.put("sentenceCount", sentences.length);
        
        String[] paragraphs = text.split("\n\n+");
        analysis.put("paragraphCount", paragraphs.length);
        
        // Language analysis
        double avgWordLength = Arrays.stream(words)
            .mapToInt(String::length)
            .average()
            .orElse(0.0);
        analysis.put("avgWordLength", avgWordLength);
        
        analysis.put("readability", calculateReadability(words.length, sentences.length));
        analysis.put("complexity", determineComplexity(avgWordLength, words.length, sentences.length));
        
        // Sentiment analysis
        SentimentResult sentiment = performSentimentAnalysis(text);
        analysis.put("sentiment", sentiment.getSentiment());
        analysis.put("sentimentScore", sentiment.getScore());
        analysis.put("positiveWords", sentiment.getPositiveCount());
        analysis.put("negativeWords", sentiment.getNegativeCount());
        
        // Generate insights
        analysis.put("insights", generateTextInsights(analysis));
        
        return analysis;
    }

    private SentimentResult performSentimentAnalysis(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z\\s]", "").split("\\s+");
        
        int positiveCount = 0;
        int negativeCount = 0;
        int neutralCount = 0;
        
        for (String word : words) {
            if (POSITIVE_WORDS.contains(word)) {
                positiveCount++;
            } else if (NEGATIVE_WORDS.contains(word)) {
                negativeCount++;
            } else {
                neutralCount++;
            }
        }
        
        double score = (double) (positiveCount - negativeCount) / words.length;
        String sentiment;
        String analysis;
        
        if (score > 0.1) {
            sentiment = "Positive";
            analysis = "The text contains predominantly positive language and sentiment.";
        } else if (score < -0.1) {
            sentiment = "Negative";
            analysis = "The text contains predominantly negative language and sentiment.";
        } else {
            sentiment = "Neutral";
            analysis = "The text maintains a balanced or neutral tone.";
        }
        
        return new SentimentResult(sentiment, Math.abs(score), positiveCount, negativeCount, neutralCount, analysis);
    }

    private List<String> performKeywordExtraction(String text) {
        String[] words = text.toLowerCase()
            .replaceAll("[^a-zA-Z\\s]", "")
            .split("\\s+");
        
        // Simple keyword extraction based on frequency
        Map<String, Integer> wordFreq = new HashMap<>();
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should");
        
        for (String word : words) {
            if (word.length() > 3 && !stopWords.contains(word)) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }
        
        return wordFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private String performTextSummarization(String text) {
        String[] sentences = text.split("[.!?]+");
        
        if (sentences.length <= 3) {
            return text.trim();
        }
        
        // Simple extractive summarization - take first, middle, and last sentences
        List<String> summary = new ArrayList<>();
        summary.add(sentences[0].trim());
        
        if (sentences.length > 2) {
            summary.add(sentences[sentences.length / 2].trim());
        }
        
        if (sentences.length > 1) {
            summary.add(sentences[sentences.length - 1].trim());
        }
        
        return summary.stream()
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(". ")) + ".";
    }

    private String calculateReadability(int wordCount, int sentenceCount) {
        if (sentenceCount == 0) return "Unknown";
        
        double avgWordsPerSentence = (double) wordCount / sentenceCount;
        
        if (avgWordsPerSentence <= 10) {
            return "Easy";
        } else if (avgWordsPerSentence <= 20) {
            return "Moderate";
        } else {
            return "Difficult";
        }
    }

    private String determineComplexity(double avgWordLength, int wordCount, int sentenceCount) {
        double complexityScore = avgWordLength + (double) wordCount / sentenceCount;
        
        if (complexityScore <= 10) {
            return "Simple";
        } else if (complexityScore <= 20) {
            return "Moderate";
        } else {
            return "Complex";
        }
    }

    private String generateTextInsights(Map<String, Object> analysis) {
        StringBuilder insights = new StringBuilder();
        
        int wordCount = (Integer) analysis.get("wordCount");
        double avgWordLength = (Double) analysis.get("avgWordLength");
        String sentiment = (String) analysis.get("sentiment");
        
        if (wordCount < 50) {
            insights.append("- Short text suitable for quick reading\n");
        } else if (wordCount > 500) {
            insights.append("- Long text that may benefit from summarization\n");
        }
        
        if (avgWordLength > 6) {
            insights.append("- Text uses sophisticated vocabulary\n");
        }
        
        insights.append("- Overall tone is ").append(sentiment.toLowerCase()).append("\n");
        
        return insights.toString();
    }

    // Supporting classes
    public static class SentimentResult {
        private String sentiment;
        private double score;
        private int positiveCount;
        private int negativeCount;
        private int neutralCount;
        private String analysis;
        
        public SentimentResult(String sentiment, double score, int positiveCount, int negativeCount, int neutralCount, String analysis) {
            this.sentiment = sentiment;
            this.score = score;
            this.positiveCount = positiveCount;
            this.negativeCount = negativeCount;
            this.neutralCount = neutralCount;
            this.analysis = analysis;
        }
        
        // Getters
        public String getSentiment() { return sentiment; }
        public double getScore() { return score; }
        public int getPositiveCount() { return positiveCount; }
        public int getNegativeCount() { return negativeCount; }
        public int getNeutralCount() { return neutralCount; }
        public String getAnalysis() { return analysis; }
    }
}
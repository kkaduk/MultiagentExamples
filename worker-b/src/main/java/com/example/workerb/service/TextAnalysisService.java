package com.example.workerb.service;

import com.example.workerb.service.model.AnalysisResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TextAnalysisService {

    public String analyze(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "[NO_CONTENT_TO_ANALYZE]";
        }
        
        // Simulate some processing time
        try {
            Thread.sleep(150 + (long)(Math.random() * 400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        AnalysisResult analysis = performAnalysis(input);
        return formatAnalysisResult(analysis);
    }

    private AnalysisResult performAnalysis(String input) {
        String cleanInput = input.trim();
        
        // Basic text statistics
        int characterCount = cleanInput.length();
        String[] words = cleanInput.split("\\s+");
        int wordCount = words.length;
        int sentenceCount = cleanInput.split("[.!?]+").length;
        
        // Word frequency analysis
        Map<String, Integer> wordFrequency = Arrays.stream(words)
                .map(word -> word.toLowerCase().replaceAll("[^a-zA-Z]", ""))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.groupingBy(
                    word -> word,
                    Collectors.summingInt(w -> 1)
                ));
        
        // Simple sentiment analysis (mock)
        String sentiment = analyzeSentiment(cleanInput);
        
        // Readability score (mock)
        double readabilityScore = calculateReadabilityScore(wordCount, sentenceCount);
        
        // Generate summary
        String summary = generateSummary(characterCount, wordCount, sentenceCount, sentiment);
        
        return AnalysisResult.builder()
                .characterCount(characterCount)
                .wordCount(wordCount)
                .sentenceCount(sentenceCount)
                .wordFrequency(wordFrequency)
                .sentiment(sentiment)
                .readabilityScore(readabilityScore)
                .summary(summary)
                .build();
    }

    private String analyzeSentiment(String input) {
        String lowercaseInput = input.toLowerCase();
        
        // Simple keyword-based sentiment analysis
        List<String> positiveWords = Arrays.asList("good", "great", "excellent", "amazing", "wonderful", "fantastic", "love", "like", "happy", "joy");
        List<String> negativeWords = Arrays.asList("bad", "terrible", "awful", "horrible", "hate", "dislike", "sad", "angry", "disappointed", "frustrated");
        
        long positiveCount = positiveWords.stream().mapToLong(word -> 
                lowercaseInput.split("\\b" + word + "\\b").length - 1).sum();
        long negativeCount = negativeWords.stream().mapToLong(word -> 
                lowercaseInput.split("\\b" + word + "\\b").length - 1).sum();
        
        if (positiveCount > negativeCount) {
            return "POSITIVE";
        } else if (negativeCount > positiveCount) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    private double calculateReadabilityScore(int wordCount, int sentenceCount) {
        if (sentenceCount == 0) return 0.0;
        
        // Simple readability calculation based on average words per sentence
        double avgWordsPerSentence = (double) wordCount / sentenceCount;
        
        // Score from 1-10 (higher is easier to read)
        if (avgWordsPerSentence <= 10) return 9.0;
        else if (avgWordsPerSentence <= 15) return 7.0;
        else if (avgWordsPerSentence <= 20) return 5.0;
        else return 3.0;
    }

    private String generateSummary(int charCount, int wordCount, int sentenceCount, String sentiment) {
        return String.format(
                "Text contains %d characters, %d words, %d sentences. Sentiment: %s",
                charCount, wordCount, sentenceCount, sentiment
        );
    }

    private String formatAnalysisResult(AnalysisResult result) {
        return String.format(
                "[ANALYSIS: %s | Score: %.1f | Top words: %s]",
                result.getSummary(),
                result.getReadabilityScore(),
                getTopWords(result.getWordFrequency(), 3)
        );
    }

    private String getTopWords(Map<String, Integer> wordFreq, int limit) {
        return wordFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    public String getDetailedStats(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "No content provided for detailed analysis";
        }
        
        AnalysisResult analysis = performAnalysis(input);
        return String.format(
            "Detailed Analysis:\n" +
            "- Characters: %d\n" +
            "- Words: %d\n" +
            "- Sentences: %d\n" +
            "- Sentiment: %s\n" +
            "- Readability Score: %.1f/10\n" +
            "- Most frequent words: %s",
            analysis.getCharacterCount(),
            analysis.getWordCount(),
            analysis.getSentenceCount(),
            analysis.getSentiment(),
            analysis.getReadabilityScore(),
            getTopWords(analysis.getWordFrequency(), 5)
        );
    }
}
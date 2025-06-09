package com.example.workera.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TextTransformationService {

    public String transform(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "[EMPTY]";
        }
        
        // Simulate some processing time
        try {
            Thread.sleep(100 + (long)(Math.random() * 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return transformText(input);
    }

    private String transformText(String input) {
        // Multiple transformation operations
        String result = input.trim();
        
        // 1. Convert to uppercase
        result = result.toUpperCase();
        
        // 2. Reverse words
        List<String> words = Arrays.asList(result.split("\\s+"));
        Collections.reverse(words);
        result = String.join(" ", words);
        
        // 3. Add prefix and suffix
        result = "[TRANSFORMED: " + result + "]";
        
        return result;
    }

    public String reverseTransform(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "[EMPTY_REVERSE]";
        }
        
        // Simple reverse operation
        return new StringBuilder(input).reverse().toString();
    }

    public String extractKeywords(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "[]";
        }
        
        // Extract words longer than 3 characters
        List<String> keywords = Arrays.stream(input.split("\\s+"))
                .filter(word -> word.length() > 3)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
        
        return "[KEYWORDS: " + String.join(", ", keywords) + "]";
    }
}
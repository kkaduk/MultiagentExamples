package com.example.workerb.service.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResult {
    private int characterCount;
    private int wordCount;
    private int sentenceCount;
    private Map<String, Integer> wordFrequency;
    private String sentiment;
    private double readabilityScore;
    private String summary;
}
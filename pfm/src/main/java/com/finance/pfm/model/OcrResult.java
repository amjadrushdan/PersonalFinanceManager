package com.finance.pfm.model;

public class OcrResult {
    private Double amount;      // extracted total (nullable)
    private String description; // extracted merchant / best description
    private double confidence;  // 0.0 - 1.0
    private String fullText;    // raw OCR text

    public OcrResult(Double amount, String description, double confidence, String fullText) {
        this.amount = amount;
        this.description = description;
        this.confidence = confidence;
        this.fullText = fullText;
    }
    // getters
    public Double getAmount() { return amount; }
    public String getDescription() { return description; }
    public double getConfidence() { return confidence; }
    public String getFullText() { return fullText; }
}

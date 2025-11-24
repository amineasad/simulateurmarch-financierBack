package com.pidev.pattern.dto;

/**
 * DTO pour les requêtes d'analyse de patterns
 */
public class PatternRequest {
    private String symbol;
    private String period;
    private boolean fullAnalysis;

    public PatternRequest() {
    }

    public PatternRequest(String symbol, String period, boolean fullAnalysis) {
        this.symbol = symbol;
        this.period = period;
        this.fullAnalysis = fullAnalysis;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public boolean isFullAnalysis() {
        return fullAnalysis;
    }

    public void setFullAnalysis(boolean fullAnalysis) {
        this.fullAnalysis = fullAnalysis;
    }
}
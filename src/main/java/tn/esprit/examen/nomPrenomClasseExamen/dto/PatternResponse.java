package tn.esprit.examen.nomPrenomClasseExamen.dto;



import java.util.List;
import java.util.Map;

/**
 * DTO pour les réponses d'analyse de patterns
 */
public class PatternResponse {
    private String symbol;
    private String signal;
    private double confidence;
    private Map<String, Object> detectedPatterns;
    private List<String> recommendations;
    private boolean success;
    private String errorMessage;

    public PatternResponse() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getDetectedPatterns() {
        return detectedPatterns;
    }

    public void setDetectedPatterns(Map<String, Object> detectedPatterns) {
        this.detectedPatterns = detectedPatterns;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

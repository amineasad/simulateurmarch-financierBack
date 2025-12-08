package tn.esprit.examen.nomPrenomClasseExamen.dto;



import java.util.List;

public class TraderAnalysisResponse {
    private List<StrategyResult> strategies;
    private boolean success;
    private String errorMessage;

    public static class StrategyResult {
        private String name;
        private String classification;
        private double winRate;
        private double profitFactor;
        private double pnlTotal;

        public StrategyResult() {
        }

        public StrategyResult(String name, String classification, double winRate, double profitFactor, double pnlTotal) {
            this.name = name;
            this.classification = classification;
            this.winRate = winRate;
            this.profitFactor = profitFactor;
            this.pnlTotal = pnlTotal;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }

        public double getWinRate() {
            return winRate;
        }

        public void setWinRate(double winRate) {
            this.winRate = winRate;
        }

        public double getProfitFactor() {
            return profitFactor;
        }

        public void setProfitFactor(double profitFactor) {
            this.profitFactor = profitFactor;
        }

        public double getPnlTotal() {
            return pnlTotal;
        }

        public void setPnlTotal(double pnlTotal) {
            this.pnlTotal = pnlTotal;
        }
    }

    public TraderAnalysisResponse() {
    }

    public TraderAnalysisResponse(List<StrategyResult> strategies, boolean success, String errorMessage) {
        this.strategies = strategies;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public List<StrategyResult> getStrategies() {
        return strategies;
    }

    public void setStrategies(List<StrategyResult> strategies) {
        this.strategies = strategies;
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
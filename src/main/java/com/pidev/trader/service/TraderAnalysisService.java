package com.pidev.trader.service;

import com.pidev.trader.dto.TraderAnalysisResponse;
import com.pidev.trader.dto.TraderAnalysisResponse.StrategyResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TraderAnalysisService {

    private static final String PYTHON_SCRIPT_PATH = "src/main/Comportement/AnalyseTrader.py";

    @Value("${trader.useFlask:false}")
    private boolean useFlask;

    @Value("${trader.flask.baseUrl:http://localhost:5001}")
    private String traderFlaskBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exécute l'analyse des comportements des traders
     * @return Résultats de l'analyse
     */
    public TraderAnalysisResponse analyzeTraderBehavior() {
        try {
            if (useFlask) {
                String url = traderFlaskBaseUrl + "/trader/analyze";
                ResponseEntity<java.util.Map> resp = restTemplate.getForEntity(url, java.util.Map.class);
                java.util.Map body = resp.getBody();
                if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                    List<StrategyResult> strategies = new ArrayList<>();
                    Object listObj = body.get("strategies");
                    if (listObj instanceof List<?>) {
                        for (Object o : (List<?>) listObj) {
                            if (o instanceof java.util.Map<?, ?> map) {
                                StrategyResult s = new StrategyResult();
                                s.setName(String.valueOf(map.getOrDefault("name", "")));
                                s.setClassification(String.valueOf(map.getOrDefault("classification", "NEUTRE")));
                                try { s.setWinRate(Double.parseDouble(String.valueOf(map.getOrDefault("winRate", 0)))); } catch (Exception ignored) {}
                                try { s.setProfitFactor(Double.parseDouble(String.valueOf(map.getOrDefault("profitFactor", 1)))); } catch (Exception ignored) {}
                                try { s.setPnlTotal(Double.parseDouble(String.valueOf(map.getOrDefault("pnlTotal", 0)))); } catch (Exception ignored) {}
                                strategies.add(s);
                            }
                        }
                    }
                    return new TraderAnalysisResponse(strategies, true, null);
                }
                String err = body != null ? String.valueOf(body.get("errorMessage")) : "Réponse Flask vide";
                return new TraderAnalysisResponse(null, false, err);
            }
            // Exécuter le script Python
            Process process = Runtime.getRuntime().exec("python " + PYTHON_SCRIPT_PATH);
            
            // Lire la sortie du script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // Attendre que le processus se termine
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Analyser la sortie et créer la réponse
                return parseScriptOutput(output.toString());
            } else {
                // Lire les erreurs si le script échoue
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
                
                return new TraderAnalysisResponse(null, false, 
                        "Erreur lors de l'exécution du script: " + errorOutput.toString());
            }
            
        } catch (IOException | InterruptedException e) {
            return new TraderAnalysisResponse(null, false, 
                    "Exception lors de l'exécution du script: " + e.getMessage());
        }
    }
    
    /**
     * Pour les besoins de démonstration, cette méthode renvoie des données simulées
     * @return Données simulées pour l'interface
     */
    public TraderAnalysisResponse getMockData() {
        List<StrategyResult> strategies = new ArrayList<>();
        
        strategies.add(new StrategyResult("Momentum", "GAGNANTE", 58.2, 1.75, 5240));
        strategies.add(new StrategyResult("Mean Reversion", "NEUTRE", 48.7, 1.12, 1850));
        strategies.add(new StrategyResult("Breakout", "À RISQUE", 42.1, 0.95, -320));
        strategies.add(new StrategyResult("Trend Following", "PERDANTE", 35.6, 0.68, -1450));
        
        return new TraderAnalysisResponse(strategies, true, null);
    }
    
    /**
     * Analyse la sortie du script Python pour extraire les résultats
     * @param output Sortie du script Python
     * @return Résultats de l'analyse
     */
    private TraderAnalysisResponse parseScriptOutput(String output) {
        // Cette méthode devrait analyser la sortie réelle du script Python
        // Pour l'instant, nous utilisons des données simulées
        return getMockData();
    }
}

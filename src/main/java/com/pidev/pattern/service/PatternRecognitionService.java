package com.pidev.pattern.service;

import com.pidev.pattern.dto.PatternRequest;
import com.pidev.pattern.dto.PatternResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service pour l'intégration de patternrecog au backend
 */
@Service
public class PatternRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(PatternRecognitionService.class);
    private static final String PYTHON_SCRIPT_PATH = "src/main/resources/pattern_recognition/patternrecog.PY";

    /**
     * Analyse les patterns pour une action donnée
     * @param request La requête contenant les informations de l'action
     * @return La réponse contenant les résultats de l'analyse
     */
    public PatternResponse analyzePatterns(PatternRequest request) {
        PatternResponse response = new PatternResponse();
        response.setSymbol(request.getSymbol());
        
        try {
            // Préparation de la commande Python
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add(PYTHON_SCRIPT_PATH);
            command.add("--symbol");
            command.add(request.getSymbol());
            
            if (request.getPeriod() != null && !request.getPeriod().isEmpty()) {
                command.add("--period");
                command.add(request.getPeriod());
            }
            
            if (request.isFullAnalysis()) {
                command.add("--full-analysis");
            }
            
            // Exécution de la commande
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            logger.info("Exécution de la commande: {}", String.join(" ", command));
            Process process = processBuilder.start();
            
            // Lecture de la sortie
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Attente de la fin du processus
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Le processus Python a dépassé le délai d'attente");
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                // Traitement de la sortie
                response.setSuccess(true);
                parseOutput(output.toString(), response);
            } else {
                response.setSuccess(false);
                response.setErrorMessage("Erreur lors de l'exécution du script Python (code " + exitCode + "): " + output);
                logger.error("Erreur lors de l'exécution du script Python (code {}): {}", exitCode, output);
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("Erreur lors de l'analyse des patterns: " + e.getMessage());
            logger.error("Erreur lors de l'analyse des patterns", e);
        }
        
        return response;
    }
    
    /**
     * Parse la sortie du script Python pour extraire les informations pertinentes
     * @param output La sortie du script Python
     * @param response La réponse à compléter
     */
    private void parseOutput(String output, PatternResponse response) {
        // Extraction du signal
        if (output.contains("Signal actuel:")) {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.contains("Signal actuel:")) {
                    response.setSignal(line.split(":")[1].trim());
                } else if (line.contains("Confiance:")) {
                    String confidenceStr = line.split(":")[1].trim();
                    if (confidenceStr.endsWith("%")) {
                        confidenceStr = confidenceStr.substring(0, confidenceStr.length() - 1);
                    }
                    response.setConfidence(Double.parseDouble(confidenceStr) / 100);
                }
            }
        }
        
        // Extraction des patterns détectés
        Map<String, Object> detectedPatterns = new HashMap<>();
        if (output.contains("PATTERNS DÉTECTÉS:")) {
            String[] sections = output.split("PATTERNS DÉTECTÉS:");
            if (sections.length > 1) {
                String patternsSection = sections[1].split("===")[0].trim();
                String[] patternLines = patternsSection.split("\n");
                for (String line : patternLines) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        detectedPatterns.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        response.setDetectedPatterns(detectedPatterns);
        
        // Extraction des recommandations
        List<String> recommendations = new ArrayList<>();
        if (output.contains("RECOMMANDATIONS:")) {
            String[] sections = output.split("RECOMMANDATIONS:");
            if (sections.length > 1) {
                String recommendationsSection = sections[1].split("===")[0].trim();
                String[] recommendationLines = recommendationsSection.split("\n");
                for (String line : recommendationLines) {
                    if (!line.trim().isEmpty()) {
                        recommendations.add(line.trim());
                    }
                }
            }
        }
        response.setRecommendations(recommendations);
    }
}
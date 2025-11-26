package com.pidev.reclamation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service pour l'intégration avec les scripts Python de traitement des réclamations.
 */
@Service
public class PythonReclamationService {

    private static final Logger logger = LoggerFactory.getLogger(PythonReclamationService.class);
    private static final String PYTHON_SCRIPTS_PATH = "src/main/resources/python";
    private static final String RECLAMATION_AGENT_SCRIPT = "reclamation-agent.py";
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${reclamations.useFlask:false}")
    private boolean useFlask;

    @Value("${reclamations.flask.baseUrl:http://localhost:5001}")
    private String flaskBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Analyse une réclamation en utilisant le script Python.
     *
     * @param texteReclamation Le texte de la réclamation à analyser
     * @return Map contenant les résultats de l'analyse
     */
    public Map<String, Object> analyserReclamation(String texteReclamation) {
        try {
            if (useFlask) {
                return callFlaskSingle(texteReclamation);
            }
            // Création d'un fichier temporaire pour stocker le texte de la réclamation
            Path tempInputFile = createTempFile("reclamation_input", ".txt", texteReclamation);
            Path tempOutputFile = Paths.get(TEMP_DIR, "reclamation_output_" + System.currentTimeMillis() + ".json");

            // Construction de la commande Python
            List<String> command = buildPythonCommand(tempInputFile.toString(), tempOutputFile.toString());

            // Exécution de la commande
            Process process = executeCommand(command);
            
            // Lecture de la sortie standard et d'erreur
            String output = readProcessOutput(process.getInputStream());
            String error = readProcessOutput(process.getErrorStream());
            
            // Attente de la fin du processus
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                logger.error("Le processus Python a été interrompu après le délai d'attente");
                return createErrorResponse("Timeout lors de l'exécution du script Python");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Erreur lors de l'exécution du script Python. Code de sortie: {}, Erreur: {}", exitCode, error);
                return createErrorResponse("Erreur lors de l'exécution du script Python: " + error);
            }
            
            // Lecture du fichier de sortie JSON
            if (Files.exists(tempOutputFile)) {
                String jsonResult = new String(Files.readAllBytes(tempOutputFile), StandardCharsets.UTF_8);
                Map<String, Object> result = objectMapper.readValue(jsonResult, Map.class);
                
                // Nettoyage des fichiers temporaires
                Files.deleteIfExists(tempInputFile);
                Files.deleteIfExists(tempOutputFile);
                
                return result;
            } else {
                logger.error("Le fichier de sortie n'a pas été créé");
                return createErrorResponse("Le fichier de sortie n'a pas été créé");
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse de la réclamation", e);
            return createErrorResponse("Erreur lors de l'analyse de la réclamation: " + e.getMessage());
        }
    }
    
    /**
     * Traite un lot de réclamations en utilisant le script Python.
     *
     * @param reclamations Liste des textes de réclamations à traiter
     * @return Liste des résultats d'analyse
     */
    public List<Map<String, Object>> traiterLotReclamations(List<String> reclamations) {
        try {
            if (useFlask) {
                return callFlaskBatch(reclamations);
            }
            // Création d'un fichier temporaire pour stocker les réclamations au format JSON
            Map<String, List<String>> inputData = new HashMap<>();
            inputData.put("reclamations", reclamations);
            String jsonInput = objectMapper.writeValueAsString(inputData);
            
            Path tempInputFile = createTempFile("reclamations_batch_input", ".json", jsonInput);
            Path tempOutputFile = Paths.get(TEMP_DIR, "reclamations_batch_output_" + System.currentTimeMillis() + ".json");

            // Construction de la commande Python
            List<String> command = buildPythonCommand(tempInputFile.toString(), tempOutputFile.toString());

            // Exécution de la commande
            Process process = executeCommand(command);
            
            // Attente de la fin du processus
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                logger.error("Le processus Python a été interrompu après le délai d'attente");
                return List.of(createErrorResponse("Timeout lors de l'exécution du script Python"));
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String error = readProcessOutput(process.getErrorStream());
                logger.error("Erreur lors de l'exécution du script Python. Code de sortie: {}, Erreur: {}", exitCode, error);
                return List.of(createErrorResponse("Erreur lors de l'exécution du script Python: " + error));
            }
            
            // Lecture du fichier de sortie JSON
            if (Files.exists(tempOutputFile)) {
                String jsonResult = new String(Files.readAllBytes(tempOutputFile), StandardCharsets.UTF_8);
                List<Map<String, Object>> results = objectMapper.readValue(jsonResult, List.class);
                
                // Nettoyage des fichiers temporaires
                Files.deleteIfExists(tempInputFile);
                Files.deleteIfExists(tempOutputFile);
                
                return results;
            } else {
                logger.error("Le fichier de sortie n'a pas été créé");
                return List.of(createErrorResponse("Le fichier de sortie n'a pas été créé"));
            }
            
        } catch (Exception e) {
            logger.error("Erreur lors du traitement du lot de réclamations", e);
            return List.of(createErrorResponse("Erreur lors du traitement du lot de réclamations: " + e.getMessage()));
        }
    }

    private Map<String, Object> callFlaskSingle(String texte) {
        String url = flaskBaseUrl + "/analyser";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> payload = Map.of("texte", texte);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
        Map result = restTemplate.postForObject(url, entity, Map.class);
        return result != null ? result : createErrorResponse("Réponse Flask vide");
    }

    private List<Map<String, Object>> callFlaskBatch(List<String> reclamations) {
        String url = flaskBaseUrl + "/traiter-lot";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, List<String>> payload = Map.of("reclamations", reclamations);
        HttpEntity<Map<String, List<String>>> entity = new HttpEntity<>(payload, headers);
        List results = restTemplate.postForObject(url, entity, List.class);
        return results != null ? results : List.of(createErrorResponse("Réponse Flask vide"));
    }
    
    /**
     * Crée un fichier temporaire avec le contenu spécifié.
     */
    private Path createTempFile(String prefix, String suffix, String content) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        return tempFile;
    }
    
    /**
     * Construit la commande pour exécuter le script Python.
     */
    private List<String> buildPythonCommand(String inputFile, String outputFile) {
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(PYTHON_SCRIPTS_PATH, RECLAMATION_AGENT_SCRIPT).toString());
        command.add("--input");
        command.add(inputFile);
        command.add("--output");
        command.add(outputFile);
        return command;
    }
    
    /**
     * Exécute une commande système.
     */
    private Process executeCommand(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        return processBuilder.start();
    }
    
    /**
     * Lit la sortie d'un processus.
     */
    private String readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
    }
    
    /**
     * Crée une réponse d'erreur.
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorMessage);
        return errorResponse;
    }
}

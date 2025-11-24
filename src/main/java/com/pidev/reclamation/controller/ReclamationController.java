package com.pidev.reclamation.controller;

import com.pidev.reclamation.service.PythonReclamationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des réclamations.
 */
@RestController
@RequestMapping("/api/reclamations")
@CrossOrigin(origins = "*")
public class ReclamationController {

    /**
     * Test endpoint to check if controller is loaded.
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ReclamationController is loaded");
    }

    private final PythonReclamationService pythonReclamationService;

    @Autowired
    public ReclamationController(PythonReclamationService pythonReclamationService) {
        this.pythonReclamationService = pythonReclamationService;
    }

    /**
     * Analyse une réclamation.
     *
     * @param request Requête contenant le texte de la réclamation
     * @return Résultat de l'analyse
     */
    @PostMapping("/analyser")
    public ResponseEntity<Map<String, Object>> analyserReclamation(@RequestBody Map<String, String> request) {
        String texteReclamation = request.get("texte");
        if (texteReclamation == null || texteReclamation.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Mock analysis for testing
        Map<String, Object> resultat = analyserMock(texteReclamation);
        return ResponseEntity.ok(resultat);
    }

    private Map<String, Object> analyserMock(String texte) {
        Map<String, Object> result = new HashMap<>();
        String lowerText = texte.toLowerCase();

        // Simple category detection
        if (lowerText.contains("crash") || lowerText.contains("problème technique")) {
            result.put("categorie", "probleme_technique");
        } else if (lowerText.contains("frais") || lowerText.contains("montant")) {
            result.put("categorie", "frais_contestes");
        } else if (lowerText.contains("retard") || lowerText.contains("délai")) {
            result.put("categorie", "retard_execution");
        } else {
            result.put("categorie", "autre");
        }

        // Simple priority (if contains urgent)
        if (lowerText.contains("urgent") || lowerText.contains("vite")) {
            result.put("priorite", "haute");
        } else if (lowerText.contains("important")) {
            result.put("priorite", "normale");
        } else {
            result.put("priorite", "basse");
        }

        // Simple response
        String response = "Nous avons bien reçu votre réclamation concernant " + result.get("categorie") + ". Nous la traiterons avec priorité " + result.get("priorite") + ". Un conseiller vous contactera bientôt.";
        result.put("reponse_suggeree", response);

        return result;
    }

    /**
     * Traite un lot de réclamations.
     *
     * @param request Requête contenant la liste des réclamations
     * @return Résultats de l'analyse
     */
    @PostMapping("/traiter-lot")
    public ResponseEntity<List<Map<String, Object>>> traiterLotReclamations(@RequestBody Map<String, List<String>> request) {
        List<String> reclamations = request.get("reclamations");
        if (reclamations == null || reclamations.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Map<String, Object>> resultats = new ArrayList<>();
        for (String reclamation : reclamations) {
            resultats.add(analyserMock(reclamation));
        }
        return ResponseEntity.ok(resultats);
    }
}
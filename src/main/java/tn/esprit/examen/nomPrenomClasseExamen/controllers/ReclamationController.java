package tn.esprit.examen.nomPrenomClasseExamen.controllers;



import tn.esprit.examen.nomPrenomClasseExamen.services.PythonReclamationService;

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
     * Analyse une réclamation via l'agent Python.
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

        Map<String, Object> resultat = pythonReclamationService.analyserReclamation(texteReclamation);
        return ResponseEntity.ok(resultat);
    }

    // Mock retiré: la logique réelle est désormais déléguée au service Python

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

        List<Map<String, Object>> resultats = pythonReclamationService.traiterLotReclamations(reclamations);
        return ResponseEntity.ok(resultats);
    }
}
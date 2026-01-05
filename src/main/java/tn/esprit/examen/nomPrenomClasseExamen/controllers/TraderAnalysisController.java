package tn.esprit.examen.nomPrenomClasseExamen.controllers;



import tn.esprit.examen.nomPrenomClasseExamen.dto.TraderAnalysisResponse;
import tn.esprit.examen.nomPrenomClasseExamen.services.TraderAnalysisService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trader-analysis")
public class TraderAnalysisController {

    private final TraderAnalysisService traderAnalysisService;

    @Autowired
    public TraderAnalysisController(TraderAnalysisService traderAnalysisService) {
        this.traderAnalysisService = traderAnalysisService;
    }

    /**
     * Récupère les résultats d'analyse des traders
     * @return Résultats de l'analyse
     */
    @GetMapping
    public ResponseEntity<TraderAnalysisResponse> getTraderAnalysis() {
        // Pour la démonstration, utiliser les données simulées
        TraderAnalysisResponse response = traderAnalysisService.getMockData();
        return ResponseEntity.ok(response);
    }

    /**
     * Exécute l'analyse des comportements des traders
     * @return Résultats de l'analyse
     */
    @PostMapping("/run")
    public ResponseEntity<TraderAnalysisResponse> runTraderAnalysis() {
        TraderAnalysisResponse response = traderAnalysisService.analyzeTraderBehavior();

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
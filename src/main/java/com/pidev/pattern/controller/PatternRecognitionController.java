package com.pidev.pattern.controller;

import com.pidev.pattern.dto.PatternRequest;
import com.pidev.pattern.dto.PatternResponse;
import com.pidev.pattern.service.PatternRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour exposer les fonctionnalités de patternrecog
 */
@RestController
@RequestMapping("/api/pattern")
@CrossOrigin("*")
public class PatternRecognitionController {

    private final PatternRecognitionService patternRecognitionService;

    @Autowired
    public PatternRecognitionController(PatternRecognitionService patternRecognitionService) {
        this.patternRecognitionService = patternRecognitionService;
    }

    /**
     * Endpoint pour analyser les patterns d'une action
     * @param request La requête contenant les informations de l'action
     * @return Les résultats de l'analyse
     */
    @PostMapping("/analyze")
    public ResponseEntity<PatternResponse> analyzePatterns(@RequestBody PatternRequest request) {
        PatternResponse response = patternRecognitionService.analyzePatterns(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour obtenir une analyse rapide d'une action
     * @param symbol Le symbole de l'action
     * @return Les résultats de l'analyse
     */
    @GetMapping("/quick-analyze/{symbol}")
    public ResponseEntity<PatternResponse> quickAnalyze(@PathVariable String symbol) {
        PatternRequest request = new PatternRequest();
        request.setSymbol(symbol);
        request.setPeriod("1y");
        request.setFullAnalysis(false);
        
        PatternResponse response = patternRecognitionService.analyzePatterns(request);
        return ResponseEntity.ok(response);
    }
}
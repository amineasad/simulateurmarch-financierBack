// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/controllers/SessionOrderBookController.java
package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.services.SessionOrderBookService;

import java.util.Map;

@RestController
@RequestMapping("/api/orderbook")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class SessionOrderBookController {

    private final SessionOrderBookService orderBookService;

    /**
     * GET /examen/api/orderbook/{sessionId}/{symbol}/depth?levels=10
     * Récupère le carnet d'ordres pour un symbole dans une session
     */
    @GetMapping("/{sessionId}/{symbol}/depth")
    public ResponseEntity<SessionOrderBookService.OrderBookDepth> getOrderBookDepth(
            @PathVariable Long sessionId,
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int levels) {
        try {
            // Valider et limiter les niveaux
            int validLevels = Math.max(1, Math.min(levels, 50));

            log.debug("📊 Récupération carnet: session={}, symbol={}, levels={}",
                    sessionId, symbol, validLevels);

            SessionOrderBookService.OrderBookDepth depth =
                    orderBookService.depth(sessionId, symbol.toUpperCase(), validLevels);

            return ResponseEntity.ok(depth);

        } catch (Exception e) {
            log.error("❌ Erreur récupération carnet: session={}, symbol={}", sessionId, symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orderbook/active
     * Récupère tous les carnets actifs (debug)
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, SessionOrderBookService.OrderBookDepth>> getActiveBooks() {
        try {
            Map<String, SessionOrderBookService.OrderBookDepth> activeBooks =
                    orderBookService.getAllActiveBooks();
            return ResponseEntity.ok(activeBooks);
        } catch (Exception e) {
            log.error("❌ Erreur récupération carnets actifs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orderbook/{sessionId}/symbols
     * Récupère la liste des symboles avec carnets actifs pour une session
     */
    @GetMapping("/{sessionId}/symbols")
    public ResponseEntity<?> getActiveSymbols(@PathVariable Long sessionId) {
        try {
            Map<String, SessionOrderBookService.OrderBookDepth> allBooks =
                    orderBookService.getAllActiveBooks();

            // Filtrer par session
            java.util.List<String> symbols = allBooks.keySet().stream()
                    .filter(key -> key.startsWith(sessionId + "#"))
                    .map(key -> key.split("#")[1])
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(symbols);
        } catch (Exception e) {
            log.error("❌ Erreur récupération symboles session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.services.HistoricalDataGenerator;
import tn.esprit.examen.nomPrenomClasseExamen.services.MarketReplayEngine;
import tn.esprit.examen.nomPrenomClasseExamen.services.TimeCompressionCalculator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ✨ Controller pour le contrôle du replay de marché
 *
 * ENDPOINTS REST :
 * - POST   /api/replay/{sessionId}/start
 * - POST   /api/replay/{sessionId}/pause
 * - POST   /api/replay/{sessionId}/stop
 * - POST   /api/replay/{sessionId}/speed
 * - POST   /api/replay/{sessionId}/seek
 * - GET    /api/replay/{sessionId}/state
 *
 * WEBSOCKET :
 * - /app/replay/{sessionId}/control → Contrôle temps réel
 * - /topic/session/{sessionId}/market-data → Flux de données
 * - /topic/session/{sessionId}/replay-stats → Stats de progression
 */
@RestController
@RequestMapping("/api/replay")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class MarketReplayController {

    private final MarketReplayEngine replayEngine;
    private final TimeCompressionCalculator compressionCalculator;
    @Autowired
    private HistoricalDataGenerator dataGenerator;

    // ========== DTOs ==========

    @Data
    public static class StartReplayRequest {
        private List<MarketReplayEngine.MarketTick> historicalData;
        private LocalDateTime virtualStartDate;
        private LocalDateTime virtualEndDate;
        private int targetDurationMinutes;
    }

    @Data
    public static class SpeedRequest {
        private double speedMultiplier; // 1.0, 10.0, 100.0, 1000.0, 2100.0
    }

    @Data
    public static class SeekRequest {
        private LocalDateTime targetDate;
    }

    @Data
    public static class RewindForwardRequest {
        private int minutes;
    }

    // ========== REST ENDPOINTS ==========

    /**
     * 🚀 Démarre une session de replay
     *
     * POST /api/replay/{sessionId}/start
     * Body: { historicalData, virtualStartDate, virtualEndDate, targetDurationMinutes }
     */
    /**
     * 🚀 Démarre un replay avec des données générées automatiquement
     *
     * POST /api/replay/{sessionId}/start-auto
     * Body: { "symbols": ["AAPL", "MSFT", "GOOGL"], "mode": "test" }
     */
    @PostMapping("/{sessionId}/start-auto")
    public ResponseEntity<?> startAutoReplay(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request) {

        try {
            log.info("🚀 Démarrage replay AUTO session {}", sessionId);

            // Récupérer les symboles
            @SuppressWarnings("unchecked")
            List<String> symbols = (List<String>) request.get("symbols");
            if (symbols == null || symbols.isEmpty()) {
                symbols = List.of("AAPL", "MSFT", "GOOGL"); // Par défaut
            }

            // Mode: "test" (1 semaine) ou "full" (1 an)
            String mode = (String) request.getOrDefault("mode", "test");

            // Générer les données
            List<MarketReplayEngine.MarketTick> historicalData;
            LocalDateTime virtualStartDate;
            LocalDateTime virtualEndDate;
            int targetDurationMinutes;

            if ("test".equals(mode)) {
                // Mode test : 1 semaine en 5 minutes
                historicalData = dataGenerator.generateTestData(symbols);
                virtualStartDate = LocalDateTime.of(2023, 1, 2, 9, 30);
                virtualEndDate = LocalDateTime.of(2023, 1, 6, 16, 0);
                targetDurationMinutes = 5;
            } else {
                // Mode complet : 1 an en 60 minutes
                virtualStartDate = LocalDateTime.of(2023, 1, 1, 9, 30);
                historicalData = dataGenerator.generateYearData(symbols, virtualStartDate);
                virtualEndDate = LocalDateTime.of(2023, 12, 31, 16, 0);
                targetDurationMinutes = 60;
            }

            // Calculer la configuration
            TimeCompressionCalculator.CompressionConfig config =
                    compressionCalculator.calculateConfig(
                            virtualStartDate,
                            virtualEndDate,
                            targetDurationMinutes
                    );

            // Démarrer le replay
            replayEngine.startReplaySession(sessionId, historicalData, config);

            return ResponseEntity.ok(Map.of(
                    "message", "Replay démarré automatiquement",
                    "mode", mode,
                    "symbols", symbols,
                    "ticksCount", historicalData.size(),
                    "config", config
            ));

        } catch (Exception e) {
            log.error("❌ Erreur démarrage auto replay", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<?> startReplay(
            @PathVariable Long sessionId,
            @RequestBody StartReplayRequest request) {

        try {
            log.info("🚀 Démarrage replay session {}", sessionId);

            // Calculer la configuration
            TimeCompressionCalculator.CompressionConfig config =
                    compressionCalculator.calculateConfig(
                            request.getVirtualStartDate(),
                            request.getVirtualEndDate(),
                            request.getTargetDurationMinutes()
                    );

            // Démarrer le replay
            replayEngine.startReplaySession(sessionId, request.getHistoricalData(), config);

            return ResponseEntity.ok(Map.of(
                    "message", "Replay démarré",
                    "config", config
            ));

        } catch (Exception e) {
            log.error("❌ Erreur démarrage replay", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ▶️ Lance la lecture
     */
    @PostMapping("/{sessionId}/play")
    public ResponseEntity<?> play(@PathVariable Long sessionId) {
        log.info("▶️ Play session {}", sessionId);
        replayEngine.play(sessionId);
        return ResponseEntity.ok(Map.of("message", "Lecture démarrée"));
    }

    /**
     * ⏸️ Met en pause
     */
    @PostMapping("/{sessionId}/pause")
    public ResponseEntity<?> pause(@PathVariable Long sessionId) {
        log.info("⏸️ Pause session {}", sessionId);
        replayEngine.pause(sessionId);
        return ResponseEntity.ok(Map.of("message", "Lecture mise en pause"));
    }

    /**
     * ⏹️ Arrête
     */
    @PostMapping("/{sessionId}/stop")
    public ResponseEntity<?> stop(@PathVariable Long sessionId) {
        log.info("⏹️ Stop session {}", sessionId);
        replayEngine.stop(sessionId);
        return ResponseEntity.ok(Map.of("message", "Replay arrêté"));
    }

    /**
     * ⚡ Change la vitesse
     *
     * POST /api/replay/{sessionId}/speed
     * Body: { speedMultiplier: 10.0 }
     */
    @PostMapping("/{sessionId}/speed")
    public ResponseEntity<?> setSpeed(
            @PathVariable Long sessionId,
            @RequestBody SpeedRequest request) {

        log.info("⚡ Changement vitesse session {} → ×{}", sessionId, request.getSpeedMultiplier());
        replayEngine.setSpeed(sessionId, request.getSpeedMultiplier());

        return ResponseEntity.ok(Map.of(
                "message", "Vitesse modifiée",
                "speedMultiplier", request.getSpeedMultiplier()
        ));
    }

    /**
     * 🎯 Saut à une date
     *
     * POST /api/replay/{sessionId}/seek
     * Body: { targetDate: "2023-06-15T10:30:00" }
     */
    @PostMapping("/{sessionId}/seek")
    public ResponseEntity<?> seekToDate(
            @PathVariable Long sessionId,
            @RequestBody SeekRequest request) {

        log.info("🎯 Seek session {} → {}", sessionId, request.getTargetDate());
        replayEngine.seekToDate(sessionId, request.getTargetDate());

        return ResponseEntity.ok(Map.of(
                "message", "Position changée",
                "targetDate", request.getTargetDate()
        ));
    }

    /**
     * ⏪ Rewind
     *
     * POST /api/replay/{sessionId}/rewind
     * Body: { minutes: 10 }
     */
    @PostMapping("/{sessionId}/rewind")
    public ResponseEntity<?> rewind(
            @PathVariable Long sessionId,
            @RequestBody RewindForwardRequest request) {

        log.info("⏪ Rewind session {} de {} min", sessionId, request.getMinutes());
        replayEngine.rewind(sessionId, request.getMinutes());

        return ResponseEntity.ok(Map.of(
                "message", "Rewind effectué",
                "minutes", request.getMinutes()
        ));
    }

    /**
     * ⏩ Forward
     *
     * POST /api/replay/{sessionId}/forward
     * Body: { minutes: 10 }
     */
    @PostMapping("/{sessionId}/forward")
    public ResponseEntity<?> forward(
            @PathVariable Long sessionId,
            @RequestBody RewindForwardRequest request) {

        log.info("⏩ Forward session {} de {} min", sessionId, request.getMinutes());
        replayEngine.forward(sessionId, request.getMinutes());

        return ResponseEntity.ok(Map.of(
                "message", "Forward effectué",
                "minutes", request.getMinutes()
        ));
    }

    /**
     * 📊 État actuel
     *
     * GET /api/replay/{sessionId}/state
     */
    @GetMapping("/{sessionId}/state")
    public ResponseEntity<?> getState(@PathVariable Long sessionId) {
        Map<String, Object> state = replayEngine.getSessionState(sessionId);
        return ResponseEntity.ok(state);
    }

    // ========== WEBSOCKET HANDLERS ==========

    /**
     * 🎮 Contrôle via WebSocket
     *
     * /app/replay/{sessionId}/control
     */
    @MessageMapping("/replay/{sessionId}/control")
    public void handleReplayControl(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> command) {

        String action = (String) command.get("action");

        log.info("🎮 Commande replay WebSocket: session={}, action={}", sessionId, action);

        switch (action) {
            case "play":
                replayEngine.play(sessionId);
                break;
            case "pause":
                replayEngine.pause(sessionId);
                break;
            case "stop":
                replayEngine.stop(sessionId);
                break;
            case "speed":
                double speed = ((Number) command.get("speedMultiplier")).doubleValue();
                replayEngine.setSpeed(sessionId, speed);
                break;
            case "seek":
                String dateStr = (String) command.get("targetDate");
                LocalDateTime targetDate = LocalDateTime.parse(dateStr);
                replayEngine.seekToDate(sessionId, targetDate);
                break;
            case "rewind":
                int rewindMin = ((Number) command.get("minutes")).intValue();
                replayEngine.rewind(sessionId, rewindMin);
                break;
            case "forward":
                int forwardMin = ((Number) command.get("minutes")).intValue();
                replayEngine.forward(sessionId, forwardMin);
                break;
            default:
                log.warn("⚠️ Action inconnue: {}", action);
        }
    }
}

/**
 * ✨ Controller WebSocket séparé pour la diffusion
 */
@Controller
@Slf4j
class MarketReplayWebSocketController {

    /**
     * Les clients s'abonnent à :
     *
     * /topic/session/{sessionId}/market-data
     * → Reçoit les ticks en temps réel
     *
     * /topic/session/{sessionId}/replay-stats
     * → Reçoit les stats de progression
     */
}
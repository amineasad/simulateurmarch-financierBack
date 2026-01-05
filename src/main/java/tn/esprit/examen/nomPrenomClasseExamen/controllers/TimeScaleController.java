// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/controllers/TimeScaleController.java
package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.TradingSession;
import tn.esprit.examen.nomPrenomClasseExamen.services.TimeScaleManager;
import tn.esprit.examen.nomPrenomClasseExamen.services.TradingSessionService;

import java.time.Instant;
import java.time.ZoneId;

/**
 * API pour récupérer les informations de compression temporelle
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/time")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class TimeScaleController {

    private final TradingSessionService sessionService;
    private final TimeScaleManager timeScaleManager;

    /**
     * GET /api/sessions/{sessionId}/time/stats
     * Récupère les statistiques temporelles en temps réel
     */
    @GetMapping("/stats")
    public ResponseEntity<TimeScaleManager.TimeScaleStats> getTimeStats(@PathVariable Long sessionId) {
        try {
            TradingSession session = sessionService.getSessionById(sessionId);

            if (session.getHeureDebut() == null) {
                return ResponseEntity.badRequest().build();
            }

            Instant sessionStart = session.getHeureDebut()
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            Instant now = Instant.now();

            TimeScaleManager.TimeScaleStats stats = timeScaleManager.getStats(sessionStart, now);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Erreur récupération time stats pour session {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/sessions/{sessionId}/time/virtual-clock
     * Retourne l'heure virtuelle actuelle du marché
     */
    @GetMapping("/virtual-clock")
    public ResponseEntity<VirtualClockResponse> getVirtualClock(@PathVariable Long sessionId) {
        try {
            TradingSession session = sessionService.getSessionById(sessionId);

            if (session.getHeureDebut() == null) {
                return ResponseEntity.ok(VirtualClockResponse.builder()
                        .virtualTime("09:30")
                        .phase("PRE_MARKET")
                        .isMarketOpen(false)
                        .progressPercentage(0.0)
                        .build());
            }

            Instant sessionStart = session.getHeureDebut()
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            Instant now = Instant.now();

            TimeScaleManager.TimeScaleStats stats = timeScaleManager.getStats(sessionStart, now);

            VirtualClockResponse response = VirtualClockResponse.builder()
                    .virtualTime(stats.getVirtualTimeFormatted())
                    .phase(stats.getTradingPhase().name())
                    .phaseDescription(stats.getTradingPhase().getDescription())
                    .isMarketOpen(stats.isMarketOpen())
                    .progressPercentage(stats.getProgressPercentage())
                    .realMinutesRemaining(stats.getRealMinutesRemaining())
                    .timeScaleFactor(stats.getTimeScaleFactor())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur récupération virtual clock", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== DTO ==========

    @lombok.Builder
    @lombok.Data
    public static class VirtualClockResponse {
        private String virtualTime;           // "10:45"
        private String phase;                 // "OPENING"
        private String phaseDescription;      // "Ouverture (volatilité haute)"
        private boolean isMarketOpen;         // true
        private double progressPercentage;    // 0.32 (32%)
        private long realMinutesRemaining;    // 45 minutes réelles
        private double timeScaleFactor;       // 6.5x
    }
}
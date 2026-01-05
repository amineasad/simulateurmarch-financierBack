// tn.esprit.examen.nomPrenomClasseExamen.controllers.SessionParticipationController

package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionParticipation;
import tn.esprit.examen.nomPrenomClasseExamen.services.SessionParticipationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/participations")
@CrossOrigin("*")
@RequiredArgsConstructor
public class SessionParticipationController {

    private final SessionParticipationService participationService;

    /**
     * POST /api/participations/join - Rejoindre une session
     * Body: { "sessionId": 1, "userId": 5 }
     */
    @PostMapping("/join")
    public ResponseEntity<SessionParticipation> joinSession(@RequestBody Map<String, Long> request) {
        try {
            Long sessionId = request.get("sessionId");
            Long userId = request.get("userId");

            SessionParticipation participation = participationService.joinSession(sessionId, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(participation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/participations/join-by-code - Rejoindre par code
     * Body: { "codeAcces": "GAME-ABC123", "userId": 5 }
     */
    @PostMapping("/join-by-code")
    public ResponseEntity<SessionParticipation> joinByCode(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("codeAcces");
            Long userId = Long.parseLong(request.get("userId"));

            SessionParticipation participation = participationService.joinSessionByCode(code, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(participation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/participations/connect - Se connecter à une session
     * Body: { "sessionId": 1, "userId": 5 }
     */
    @PostMapping("/connect")
    public ResponseEntity<SessionParticipation> connect(@RequestBody Map<String, Long> request) {
        try {
            Long sessionId = request.get("sessionId");
            Long userId = request.get("userId");

            SessionParticipation participation = participationService.connect(sessionId, userId);
            return ResponseEntity.ok(participation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/participations/disconnect - Se déconnecter
     * Body: { "sessionId": 1, "userId": 5 }
     */
    @PostMapping("/disconnect")
    public ResponseEntity<SessionParticipation> disconnect(@RequestBody Map<String, Long> request) {
        try {
            Long sessionId = request.get("sessionId");
            Long userId = request.get("userId");

            SessionParticipation participation = participationService.disconnect(sessionId, userId);
            return ResponseEntity.ok(participation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/participations/session/{sessionId} - Tous les participants
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SessionParticipation>> getParticipants(@PathVariable Long sessionId) {
        return ResponseEntity.ok(participationService.getParticipants(sessionId));
    }

    /**
     * GET /api/participations/session/{sessionId}/leaderboard - Classement
     */
    @GetMapping("/session/{sessionId}/leaderboard")
    public ResponseEntity<List<SessionParticipation>> getLeaderboard(@PathVariable Long sessionId) {
        return ResponseEntity.ok(participationService.getLeaderboard(sessionId));
    }

    /**
     * GET /api/participations/session/{sessionId}/user/{userId} - Participation spécifique
     */
    @GetMapping("/session/{sessionId}/user/{userId}")
    public ResponseEntity<SessionParticipation> getParticipation(
            @PathVariable Long sessionId,
            @PathVariable Long userId) {
        try {
            return ResponseEntity.ok(participationService.getParticipation(sessionId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/participations/session/{sessionId}/active-count - Nombre de connectés
     */
    @GetMapping("/session/{sessionId}/active-count")
    public ResponseEntity<Long> countActiveParticipants(@PathVariable Long sessionId) {
        return ResponseEntity.ok(participationService.countActiveParticipants(sessionId));
    }

    /**
     * PUT /api/participations/update-portfolio - Mettre à jour le portefeuille
     * Body: { "sessionId": 1, "userId": 5, "cashActuel": 95000, "valeurPortefeuille": 105000 }
     */
    @PutMapping("/update-portfolio")
    public ResponseEntity<SessionParticipation> updatePortfolio(@RequestBody Map<String, Object> request) {
        try {
            Long sessionId = ((Number) request.get("sessionId")).longValue();
            Long userId = ((Number) request.get("userId")).longValue();
            Double cashActuel = ((Number) request.get("cashActuel")).doubleValue();
            Double valeurPortefeuille = ((Number) request.get("valeurPortefeuille")).doubleValue();

            SessionParticipation updated = participationService.updatePortfolio(
                    sessionId, userId, cashActuel, valeurPortefeuille
            );
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
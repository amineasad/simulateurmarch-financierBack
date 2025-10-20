// tn.esprit.examen.nomPrenomClasseExamen.controllers.TradingSessionController

package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.TradingSession;
import tn.esprit.examen.nomPrenomClasseExamen.services.TradingSessionService;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin("*")
@RequiredArgsConstructor
public class TradingSessionController {

    private final TradingSessionService sessionService;

    /**
     * POST /api/sessions - Créer une session
     */
    @PostMapping
    public ResponseEntity<TradingSession> createSession(@RequestBody TradingSession session) {
        try {
            TradingSession created = sessionService.createSession(session);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/sessions - Toutes les sessions
     */
    @GetMapping
    public ResponseEntity<List<TradingSession>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    /**
     * GET /api/sessions/active - Sessions actives
     */
    @GetMapping("/active")
    public ResponseEntity<List<TradingSession>> getActiveSessions() {
        return ResponseEntity.ok(sessionService.getActiveSessions());
    }

    /**
     * GET /api/sessions/{id} - Session par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TradingSession> getSessionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sessionService.getSessionById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/sessions/code/{code} - Session par code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<TradingSession> getSessionByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(sessionService.getSessionByCode(code));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/sessions/creator/{creatorId} - Sessions d'un créateur
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<TradingSession>> getSessionsByCreator(@PathVariable Long creatorId) {
        return ResponseEntity.ok(sessionService.getSessionsByCreator(creatorId));
    }

    /**
     * POST /api/sessions/{id}/start - Démarrer
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<TradingSession> startSession(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sessionService.startSession(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/sessions/{id}/pause - Pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<TradingSession> pauseSession(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sessionService.pauseSession(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/sessions/{id}/resume - Reprendre
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<TradingSession> resumeSession(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sessionService.resumeSession(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/sessions/{id}/close - Fermer
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<TradingSession> closeSession(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sessionService.closeSession(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/sessions/{id} - Mettre à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<TradingSession> updateSession(@PathVariable Long id, @RequestBody TradingSession session) {
        try {
            return ResponseEntity.ok(sessionService.updateSession(id, session));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/sessions/{id} - Supprimer
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        try {
            sessionService.deleteSession(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/sessions/{id}/participants/count - Compter participants
     */
    @GetMapping("/{id}/participants/count")
    public ResponseEntity<Long> countParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.countParticipants(id));
    }

    /**
     * GET /api/sessions/{id}/is-full - Vérifier si pleine
     */
    @GetMapping("/{id}/is-full")
    public ResponseEntity<Boolean> isSessionFull(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.isSessionFull(id));
    }
}
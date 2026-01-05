// tn.esprit.examen.nomPrenomClasseExamen.controllers.MarketEventController

package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.MarketEvent;
import tn.esprit.examen.nomPrenomClasseExamen.services.MarketEventService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin("*")
@RequiredArgsConstructor
public class MarketEventController {

    private final MarketEventService eventService;

    /**
     * POST /api/events - Créer un événement
     */
    @PostMapping
    public ResponseEntity<MarketEvent> createEvent(@RequestBody MarketEvent event) {
        try {
            MarketEvent created = eventService.createEvent(event);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/events/session/{sessionId} - Événements d'une session
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<MarketEvent>> getSessionEvents(@PathVariable Long sessionId) {
        return ResponseEntity.ok(eventService.getSessionEvents(sessionId));
    }

    /**
     * GET /api/events/{id} - Événement par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MarketEvent> getEventById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventService.getEventById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/events/{id}/trigger - Déclencher un événement
     */
// MarketEventController.java
    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> triggerEvent(@PathVariable Long id) {
        eventService.triggerEvent(id);
        return ResponseEntity.noContent().build(); // 204
    }


    /**
     * POST /api/events/session/{sessionId}/check-scheduled - Vérifier événements programmés
     */
    @PostMapping("/session/{sessionId}/check-scheduled")
    public ResponseEntity<List<MarketEvent>> checkScheduledEvents(@PathVariable Long sessionId) {
        List<MarketEvent> triggered = eventService.checkAndTriggerScheduledEvents(sessionId);
        return ResponseEntity.ok(triggered);
    }

    /**
     * DELETE /api/events/{id} - Supprimer un événement
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
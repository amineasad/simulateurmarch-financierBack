// tn.esprit.examen.nomPrenomClasseExamen.controllers.SessionOrderController

package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionOrder;
import tn.esprit.examen.nomPrenomClasseExamen.services.SessionOrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin("*")
@RequiredArgsConstructor
public class SessionOrderController {

    private final SessionOrderService orderService;

    /**
     * POST /api/orders - Passer un ordre
     */
    @PostMapping
    public ResponseEntity<SessionOrder> placeOrder(@RequestBody SessionOrder order) {
        try {
            SessionOrder placed = orderService.placeOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(placed);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/orders/{id}/execute - Exécuter un ordre
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<SessionOrder> executeOrder(@PathVariable Long id) {
        try {
            SessionOrder executed = orderService.executeOrder(id);
            return ResponseEntity.ok(executed);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/orders/{id}/cancel - Annuler un ordre
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<SessionOrder> cancelOrder(@PathVariable Long id) {
        try {
            SessionOrder cancelled = orderService.cancelOrder(id);
            return ResponseEntity.ok(cancelled);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/orders/{id} - Ordre par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionOrder> getOrderById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/orders/session/{sessionId} - Tous les ordres d'une session
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SessionOrder>> getSessionOrders(@PathVariable Long sessionId) {
        return ResponseEntity.ok(orderService.getSessionOrders(sessionId));
    }

    /**
     * GET /api/orders/session/{sessionId}/user/{userId} - Ordres d'un utilisateur
     */
    @GetMapping("/session/{sessionId}/user/{userId}")
    public ResponseEntity<List<SessionOrder>> getUserOrders(
            @PathVariable Long sessionId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(sessionId, userId));
    }

    /**
     * GET /api/orders/session/{sessionId}/activity - Feed d'activité
     */
    @GetMapping("/session/{sessionId}/activity")
    public ResponseEntity<List<SessionOrder>> getActivityFeed(@PathVariable Long sessionId) {
        return ResponseEntity.ok(orderService.getActivityFeed(sessionId));
    }

    /**
     * GET /api/orders/session/{sessionId}/volume - Volume total
     */
    @GetMapping("/session/{sessionId}/volume")
    public ResponseEntity<Double> getTotalVolume(@PathVariable Long sessionId) {
        return ResponseEntity.ok(orderService.getTotalVolume(sessionId));
    }
}
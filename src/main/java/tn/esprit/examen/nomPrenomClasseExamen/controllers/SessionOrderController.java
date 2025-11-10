// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/controllers/SessionOrderController.java
package tn.esprit.examen.nomPrenomClasseExamen.controllers;
import tn.esprit.examen.nomPrenomClasseExamen.dto.OrderResponseDTO;
import java.net.URI;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionOrder;
import tn.esprit.examen.nomPrenomClasseExamen.services.SessionOrderBookService;
import tn.esprit.examen.nomPrenomClasseExamen.services.SessionOrderService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class SessionOrderController {

    private final SessionOrderService orderService;
    private final SessionOrderBookService orderBookService;

    /**
     * POST /examen/api/orders
     * Place un nouvel ordre
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> placeOrder(@RequestBody SessionOrder order) {
        try {
            log.info("📥 Réception ordre: {} {} {} @ {}", order.getSide(), order.getQuantity(), order.getSymbol(), order.getPrice());

            SessionOrder saved = orderService.placeOrder(order);

            if (saved.getStatus() == OrderStatus.PENDING) {
                orderBookService.submit(saved);
                saved = orderService.getOrderById(saved.getId());
            }

            OrderResponseDTO out = toDto(saved);
            URI location = URI.create("/examen/api/orders/" + saved.getId());
            return ResponseEntity.created(location).body(out);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("❌ Rejet d'ordre: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "timestamp", Instant.now().toString(),
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur serveur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "timestamp", Instant.now().toString(),
                    "message", "Erreur serveur: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /examen/api/orders/{id}
     * Récupère un ordre par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            SessionOrder order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("❌ Ordre introuvable: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    // ========= Mapper privé =========
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private OrderResponseDTO toDto(SessionOrder o) {
        return OrderResponseDTO.builder()
                .id(o.getId())
                .sessionId(o.getSession() != null ? o.getSession().getId() : o.getSessionId())
                .userId(o.getUser() != null ? o.getUser().getId() : o.getUserId())
                .symbol(o.getSymbol())
                .type(o.getType() != null ? o.getType().name() : null)
                .side(o.getSide() != null ? o.getSide().name() : null)
                .quantity(o.getQuantity())
                .filledQuantity(o.getFilledQuantity())
                .remainingQuantity(o.getRemainingQuantity())
                .price(o.getPrice())
                .executionPrice(o.getExecutionPrice())
                .status(o.getStatus() != null ? o.getStatus().name() : null)
                .rejectionReason(o.getRejectionReason())
                .orderTime(o.getOrderTime() != null ? o.getOrderTime().format(ISO) : null)
                .executionTime(o.getExecutionTime() != null ? o.getExecutionTime().format(ISO) : null)
                .build();
    }

    /**
     * GET /examen/api/orders/session/{sessionId}
     * Récupère tous les ordres d'une session
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SessionOrder>> getSessionOrders(@PathVariable Long sessionId) {
        try {
            List<SessionOrder> orders = orderService.getSessionOrders(sessionId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("❌ Erreur récupération ordres session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orders/session/{sessionId}/user/{userId}
     * Récupère les ordres d'un utilisateur dans une session
     */
    @GetMapping("/session/{sessionId}/user/{userId}")
    public ResponseEntity<List<SessionOrder>> getUserOrders(
            @PathVariable Long sessionId,
            @PathVariable Long userId) {
        try {
            List<SessionOrder> orders = orderService.getUserOrders(sessionId, userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("❌ Erreur récupération ordres user {} session {}", userId, sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orders/session/{sessionId}/user/{userId}/active
     * Récupère les ordres actifs d'un utilisateur
     */
    @GetMapping("/session/{sessionId}/user/{userId}/active")
    public ResponseEntity<List<SessionOrder>> getUserActiveOrders(
            @PathVariable Long sessionId,
            @PathVariable Long userId) {
        try {
            List<SessionOrder> orders = orderService.getUserActiveOrders(sessionId, userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("❌ Erreur récupération ordres actifs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orders/session/{sessionId}/active
     * Récupère tous les ordres actifs d'une session
     */
    @GetMapping("/session/{sessionId}/active")
    public ResponseEntity<List<SessionOrder>> getActiveOrders(@PathVariable Long sessionId) {
        try {
            List<SessionOrder> orders = orderService.getActiveOrders(sessionId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("❌ Erreur récupération ordres actifs session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orders/session/{sessionId}/activity
     * Récupère le feed d'activité (ordres récemment exécutés)
     */
    @GetMapping("/session/{sessionId}/activity")
    public ResponseEntity<List<SessionOrder>> getActivityFeed(@PathVariable Long sessionId) {
        try {
            List<SessionOrder> activity = orderService.getActivityFeed(sessionId);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            log.error("❌ Erreur récupération activité session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orders/session/{sessionId}/volume
     * Récupère le volume total tradé dans une session
     */
    @GetMapping("/session/{sessionId}/volume")
    public ResponseEntity<Map<String, Double>> getTotalVolume(@PathVariable Long sessionId) {
        try {
            Double volume = orderService.getTotalVolume(sessionId);
            Map<String, Double> response = new HashMap<>();
            response.put("totalVolume", volume);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Erreur calcul volume session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /examen/api/orders/session/{sessionId}/stats
     * Récupère les statistiques des ordres d'une session
     */
    @GetMapping("/session/{sessionId}/stats")
    public ResponseEntity<Map<String, Object>> getSessionStats(@PathVariable Long sessionId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", orderService.countOrdersByStatus(sessionId, OrderStatus.PENDING));
            stats.put("executed", orderService.countOrdersByStatus(sessionId, OrderStatus.EXECUTED));
            stats.put("cancelled", orderService.countOrdersByStatus(sessionId, OrderStatus.CANCELLED));
            stats.put("rejected", orderService.countOrdersByStatus(sessionId, OrderStatus.REJECTED));
            stats.put("totalVolume", orderService.getTotalVolume(sessionId));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Erreur calcul stats session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /examen/api/orders/{id}/cancel
     * Annule un ordre
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            SessionOrder cancelled = orderService.cancelOrder(id);
            log.info("✅ Ordre #{} annulé", id);
            return ResponseEntity.ok(cancelled);
        } catch (RuntimeException e) {
            log.error("❌ Impossible d'annuler l'ordre {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /examen/api/orders/{id}/execute
     * Exécute manuellement un ordre (legacy/debug)
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<?> executeOrder(@PathVariable Long id) {
        try {
            SessionOrder executed = orderService.executeOrder(id);
            log.info("✅ Ordre #{} exécuté manuellement", id);
            return ResponseEntity.ok(executed);
        } catch (RuntimeException e) {
            log.error("❌ Impossible d'exécuter l'ordre {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Utilitaire pour créer une réponse d'erreur
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", java.time.LocalDateTime.now().toString());
        return error;
    }
}
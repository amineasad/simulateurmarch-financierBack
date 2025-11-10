// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/controllers/TradingWebSocketController.java
package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import tn.esprit.examen.nomPrenomClasseExamen.entities.*;
import tn.esprit.examen.nomPrenomClasseExamen.services.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TradingWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final TradingSessionService sessionService;
    private final SessionOrderService orderService;
    private final SessionOrderBookService orderBookService;
    private final AuthService authService;

    // Présence : sessionId -> Set<userId>
    private final Map<Long, Set<Long>> sessionConnectedUsers = new ConcurrentHashMap<>();

    // ===== PRÉSENCE =====

    /**
     * Gère la connexion/déconnexion d'un utilisateur à une session
     */
    @MessageMapping("/session/{sessionId}/connection")
    public void handleConnection(
            @DestinationVariable Long sessionId,
            @Payload ConnectionMessage message) {

        sessionConnectedUsers.putIfAbsent(sessionId, ConcurrentHashMap.newKeySet());
        Set<Long> users = sessionConnectedUsers.get(sessionId);

        if (message.isConnected()) {
            users.add(message.getUserId());
            log.info("✅ User {} connecté à session {}", message.getUserId(), sessionId);
        } else {
            users.remove(message.getUserId());
            log.info("❌ User {} déconnecté de session {}", message.getUserId(), sessionId);
        }

        broadcastParticipantUpdate(sessionId);
    }

    /**
     * Diffuse la mise à jour de la liste des participants
     */
    private void broadcastParticipantUpdate(Long sessionId) {
        Set<Long> users = sessionConnectedUsers.getOrDefault(sessionId, Collections.emptySet());

        ParticipantUpdate update = new ParticipantUpdate();
        update.setSessionId(sessionId);
        update.setConnectedCount(users.size());
        update.setConnectedUserIds(new ArrayList<>(users));

        messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/participants",
                update
        );

        log.debug("📢 Broadcast participants: {} users in session {}", users.size(), sessionId);
    }

    // ===== CHAT =====

    /**
     * Gère les messages de chat
     */
    @MessageMapping("/session/{sessionId}/chat")
    @SendTo("/topic/session/{sessionId}/chat")
    public ChatMessage handleChatMessage(
            @DestinationVariable Long sessionId,
            @Payload ChatMessage message) {

        message.setTime(LocalDateTime.now().toString());
        log.debug("💬 Chat message in session {}: {} - {}", sessionId, message.getUser(), message.getMessage());
        return message;
    }

    // ===== ORDRES VIA WEBSOCKET =====

    /**
     * Gère le placement d'un ordre via WebSocket
     */
    @MessageMapping("/session/{sessionId}/order")
    @SendTo("/topic/session/{sessionId}/orders")
    public OrderUpdate handleOrder(
            @DestinationVariable Long sessionId,
            @Payload OrderRequest req) {

        log.info("📥 WS Order: {} {} {} @ {}", req.getSide(), req.getQuantity(), req.getSymbol(), req.getPrice());

        try {
            // Vérifier que la session existe
            sessionService.getSessionById(sessionId);

            // Créer l'ordre
            SessionOrder order = new SessionOrder();
            order.setSessionId(sessionId);
            order.setUserId(req.getUserId());
            order.setSymbol(req.getSymbol().toUpperCase());
            order.setType(req.getType());
            order.setSide(req.getSide());
            order.setQuantity(req.getQuantity());
            order.setPrice(req.getPrice());
            order.setStatus(OrderStatus.PENDING);

            // 1. Persister et valider
            SessionOrder savedOrder = orderService.placeOrder(order);

            // 2. Soumettre au carnet si accepté
            if (savedOrder.getStatus() == OrderStatus.PENDING) {
                orderBookService.submit(savedOrder);
                savedOrder = orderService.getOrderById(savedOrder.getId());
            }

            // 3. Broadcast le carnet mis à jour
            SessionOrderBookService.OrderBookDepth depth =
                    orderBookService.depth(sessionId, req.getSymbol().toUpperCase(), 10);

            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId + "/orderbook/" + req.getSymbol().toUpperCase(),
                    depth
            );

            // 4. Construire la réponse
            String userName = getUserName(req.getUserId());

            OrderUpdate update = new OrderUpdate();
            update.setOrder(savedOrder);
            update.setUserName(userName);

            if (savedOrder.getStatus() == OrderStatus.REJECTED) {
                update.setMessage("❌ Ordre rejeté: " + savedOrder.getRejectionReason());
            } else if (savedOrder.getStatus() == OrderStatus.EXECUTED) {
                update.setMessage("✅ Ordre exécuté complètement");
            } else {
                update.setMessage("⏳ Ordre accepté (partiellement exécuté: " +
                        savedOrder.getFilledQuantity() + "/" + savedOrder.getQuantity() + ")");
            }

            log.info("✅ WS Order #{} processed: {}", savedOrder.getId(), update.getMessage());
            return update;

        } catch (Exception e) {
            log.error("❌ Erreur traitement ordre WS", e);

            OrderUpdate errorUpdate = new OrderUpdate();
            errorUpdate.setUserName(getUserName(req.getUserId()));
            errorUpdate.setMessage("❌ Erreur: " + e.getMessage());
            return errorUpdate;
        }
    }

    /**
     * Récupère le nom d'un utilisateur
     */
    private String getUserName(Long userId) {
        try {
            User user = authService.getUserById(userId);
            return (user.getNom() + " " + user.getPrenom()).trim();
        } catch (Exception e) {
            log.warn("⚠️ Impossible de récupérer le nom de l'utilisateur {}", userId);
            return "User#" + userId;
        }
    }

    // ===== BROADCAST D'ÉVÉNEMENTS =====

    /**
     * Diffuse un événement de marché
     */
    public void broadcastMarketEvent(Long sessionId, MarketEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/events",
                event
        );
        log.info("📢 Broadcast market event to session {}: {}", sessionId, event.getType());
    }

    /**
     * Diffuse une mise à jour de carnet
     */
    public void broadcastOrderBookUpdate(Long sessionId, String symbol) {
        try {
            SessionOrderBookService.OrderBookDepth depth =
                    orderBookService.depth(sessionId, symbol.toUpperCase(), 10);

            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId + "/orderbook/" + symbol.toUpperCase(),
                    depth
            );

            log.debug("📢 Broadcast orderbook update: session={}, symbol={}", sessionId, symbol);
        } catch (Exception e) {
            log.error("❌ Erreur broadcast orderbook", e);
        }
    }

    // ===== DTOs =====

    public static class ConnectionMessage {
        private Long userId;
        private boolean connected;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
    }

    public static class OrderRequest {
        private Long userId;
        private String symbol;
        private OrderType type;
        private OrderSide side;
        private Integer quantity;
        private Double price;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public OrderType getType() { return type; }
        public void setType(OrderType type) { this.type = type; }
        public OrderSide getSide() { return side; }
        public void setSide(OrderSide side) { this.side = side; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }

    public static class OrderUpdate {
        private SessionOrder order;
        private String userName;
        private String message;

        public SessionOrder getOrder() { return order; }
        public void setOrder(SessionOrder order) { this.order = order; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ChatMessage {
        private String user;
        private String message;
        private String time;

        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    public static class ParticipantUpdate {
        private Long sessionId;
        private int connectedCount;
        private List<Long> connectedUserIds;

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public int getConnectedCount() { return connectedCount; }
        public void setConnectedCount(int connectedCount) { this.connectedCount = connectedCount; }
        public List<Long> getConnectedUserIds() { return connectedUserIds; }
        public void setConnectedUserIds(List<Long> connectedUserIds) { this.connectedUserIds = connectedUserIds; }
    }
}



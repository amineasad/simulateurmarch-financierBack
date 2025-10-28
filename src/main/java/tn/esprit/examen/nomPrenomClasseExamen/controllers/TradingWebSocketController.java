package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import tn.esprit.examen.nomPrenomClasseExamen.entities.*;
import tn.esprit.examen.nomPrenomClasseExamen.services.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class TradingWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TradingSessionService sessionService;

    @Autowired
    private SessionOrderService orderService;

    @Autowired
    private SessionParticipationService participationService;

    @Autowired
    private MarketEventService eventService;

    @Autowired
    private AuthService authService; // pour récupérer les utilisateurs

    private final Map<Long, Set<Long>> sessionConnectedUsers = new ConcurrentHashMap<>();
    private final Map<String, MarketData> marketPrices = new ConcurrentHashMap<>();

    public TradingWebSocketController() {
        initializeMarketPrices();
    }

    private void initializeMarketPrices() {
        marketPrices.put("AAPL", new MarketData("AAPL", 175.43, 2.35, 1.36));
        marketPrices.put("MSFT", new MarketData("MSFT", 378.85, -3.12, -0.82));
        marketPrices.put("GOOGL", new MarketData("GOOGL", 139.75, 1.89, 1.37));
        marketPrices.put("AMZN", new MarketData("AMZN", 145.32, -2.45, -1.66));
        marketPrices.put("TSLA", new MarketData("TSLA", 238.45, 5.67, 2.44));
    }

    // ==================== CONNEXION/DECONNEXION ====================
    @MessageMapping("/session/{sessionId}/connection")
    public void handleConnection(@DestinationVariable Long sessionId,
                                 @Payload ConnectionMessage message) {

        sessionConnectedUsers.putIfAbsent(sessionId, ConcurrentHashMap.newKeySet());
        Set<Long> users = sessionConnectedUsers.get(sessionId);

        if (message.isConnected()) {
            users.add(message.getUserId());
            System.out.println("✅ User " + message.getUserId() + " connected to session " + sessionId);
        } else {
            users.remove(message.getUserId());
            System.out.println("❌ User " + message.getUserId() + " disconnected from session " + sessionId);
        }

        broadcastParticipantUpdate(sessionId);
    }

    // ==================== ORDRES ====================
    @MessageMapping("/session/{sessionId}/order")
    @SendTo("/topic/session/{sessionId}/orders")
    public OrderUpdate handleOrder(@DestinationVariable Long sessionId,
                                   @Payload OrderRequest orderRequest) {

        TradingSession sessionEntity = sessionService.getSessionById(sessionId);

        User userEntity = authService.getUserById(orderRequest.getUserId());

        SessionOrder order = new SessionOrder();
        order.setSession(sessionEntity);
        order.setUser(userEntity);
        order.setSymbol(orderRequest.getSymbol());
        order.setType(orderRequest.getType());
        order.setSide(orderRequest.getSide());
        order.setQuantity(orderRequest.getQuantity());
        order.setPrice(orderRequest.getPrice());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderTime(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        SessionOrder savedOrder = orderService.placeOrder(order);

        executeOrder(savedOrder);
        updateMarketPrice(sessionId, orderRequest.getSymbol(), orderRequest.getSide(), orderRequest.getQuantity());

        OrderUpdate update = new OrderUpdate();
        update.setOrder(savedOrder);
        update.setUserName(userEntity.getNom() + " " + userEntity.getPrenom());
        update.setMessage("Ordre exécuté");

        return update;
    }

    private void executeOrder(SessionOrder order) {
        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutionTime(LocalDateTime.now());
        order.setExecutionPrice(order.getPrice());
        orderService.executeOrder(order.getId());
    }

    private void updateMarketPrice(Long sessionId, String symbol, OrderSide side, int quantity) {
        MarketData data = marketPrices.get(symbol);
        if (data != null) {
            double impact = quantity * 0.001;
            if (side == OrderSide.BUY) {
                data.price *= (1 + impact);
                data.change = data.price * impact;
            } else {
                data.price *= (1 - impact);
                data.change = -data.price * impact;
            }
            data.changePercent = (data.change / data.price) * 100;
            broadcastMarketUpdate(sessionId, data);
        }
    }

    // ==================== CHAT ====================
    @MessageMapping("/session/{sessionId}/chat")
    @SendTo("/topic/session/{sessionId}/chat")
    public ChatMessage handleChatMessage(@DestinationVariable Long sessionId,
                                         @Payload ChatMessage message) {
        message.setTime(LocalDateTime.now().toString());
        return message;
    }

    // ==================== BROADCAST ====================
    private void broadcastMarketUpdate(Long sessionId, MarketData data) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/market", data);
    }

    private void broadcastParticipantUpdate(Long sessionId) {
        Set<Long> users = sessionConnectedUsers.getOrDefault(sessionId, Collections.emptySet());
        ParticipantUpdate update = new ParticipantUpdate();
        update.setSessionId(sessionId);
        update.setConnectedCount(users.size());
        update.setConnectedUserIds(new ArrayList<>(users));
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", update);
    }

    public void broadcastEventTrigger(Long sessionId, MarketEvent event) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/events", event);
    }

    // ==================== SCHEDULED TASKS ====================
    @Scheduled(fixedRate = 5000)
    public void simulateMarketMovements() {
        List<TradingSession> activeSessions = sessionService.getActiveSessions();
        for (TradingSession session : activeSessions) {
            marketPrices.forEach((symbol, data) -> {
                double variation = (Math.random() - 0.5) * 0.02;
                data.price *= (1 + variation);
                data.change = data.price * variation;
                data.changePercent = variation * 100;
                broadcastMarketUpdate(session.getId(), data);
            });
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkScheduledEvents() {
        List<TradingSession> activeSessions = sessionService.getActiveSessions();
        for (TradingSession session : activeSessions) {
            List<MarketEvent> triggeredEvents = eventService.checkAndTriggerScheduledEvents(session.getId());
            for (MarketEvent event : triggeredEvents) {
                applyEventImpact(session.getId(), event);
                broadcastEventTrigger(session.getId(), event);
            }
        }
    }

    private void applyEventImpact(Long sessionId, MarketEvent event) {
        marketPrices.forEach((symbol, data) -> {
            double impact = 0.01;
            data.price *= (1 + impact);
            data.change = data.price * impact;
            data.changePercent = impact * 100;
            broadcastMarketUpdate(sessionId, data);
        });
    }

    // ==================== DTO HELPERS ====================
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
        private int quantity;
        private double price;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public OrderType getType() { return type; }
        public void setType(OrderType type) { this.type = type; }
        public OrderSide getSide() { return side; }
        public void setSide(OrderSide side) { this.side = side; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
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

    public static class MarketData {
        private String symbol;
        private double price;
        private double change;
        private double changePercent;
        public MarketData(String symbol, double price, double change, double changePercent) {
            this.symbol = symbol;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
        }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public double getChange() { return change; }
        public void setChange(double change) { this.change = change; }
        public double getChangePercent() { return changePercent; }
        public void setChangePercent(double changePercent) { this.changePercent = changePercent; }
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

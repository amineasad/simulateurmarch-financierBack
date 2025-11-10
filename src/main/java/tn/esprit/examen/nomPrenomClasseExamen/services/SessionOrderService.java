// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/services/SessionOrderService.java
package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.*;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.SessionOrderRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.SessionPositionRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionOrderService {

    private final SessionOrderRepository orderRepository;
    private final TradingSessionService sessionService;
    private final SessionParticipationService participationService;
    private final SessionPositionRepository positionRepository;
    private final UserRepository userRepository;

    @Transactional
    public SessionOrder placeOrder(SessionOrder order) {
        log.info("📝 Placement ordre: {} {} {} @ {}",
                order.getSide(), order.getQuantity(), order.getSymbol(), order.getPrice());

        // ===== IDs =====
        Long sessionId = (order.getSession() != null && order.getSession().getId() != null)
                ? order.getSession().getId() : order.getSessionId();
        Long userId = (order.getUser() != null && order.getUser().getId() != null)
                ? order.getUser().getId() : order.getUserId();

        if (sessionId == null || userId == null) {
            throw new RuntimeException("SessionId et UserId sont requis"); // 400 côté controller
        }

        // ===== Chargement entités =====
        TradingSession session = sessionService.getSessionById(sessionId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + userId));

        order.setSession(session);
        order.setUser(user);

        // Normalisation symbole
        if (order.getSymbol() != null) order.setSymbol(order.getSymbol().toUpperCase());

        // ===== Validations "soft" → REJECTED si invalide =====
        String vErr = validateOrder(order);
        if (vErr != null) {
            return reject(order, vErr);
        }

        // Init défauts
        initializeDefaults(order);

        // ===== Participation (pas de createIfMissing) =====
        SessionParticipation part;
        try {
            part = participationService.getParticipation(sessionId, userId);
        } catch (Exception e) {
            return reject(order, "Participation introuvable pour cette session (rejoins la session avant de trader).");
        }

        // ===== Règles métier minimales (facultatif) =====
        String bErr = businessChecks(order, part);
        if (bErr != null) {
            return reject(order, bErr);
        }

        // Persiste PENDING
        return orderRepository.save(order);
    }

    private String validateOrder(SessionOrder o) {
        if (o.getSymbol() == null || o.getSymbol().isBlank()) return "Symbole requis";
        if (o.getQuantity() == null || o.getQuantity() <= 0)  return "Quantité invalide";
        if (o.getType() == null)                               return "Type d'ordre requis";
        if (o.getSide() == null)                               return "Side requis (BUY/SELL)";

        if (o.getType() == OrderType.LIMIT) {
            if (o.getPrice() == null || o.getPrice() <= 0)     return "Prix limite invalide";
        } else if (o.getType() == OrderType.MARKET) {
            if (o.getPrice() == null || o.getPrice() < 0)      o.setPrice(0.0); // indicatif
        }
        return null;
    }

    private void initializeDefaults(SessionOrder o) {
        if (o.getStatus() == null) o.setStatus(OrderStatus.PENDING);
        if (o.getOrderTime() == null) o.setOrderTime(LocalDateTime.now());
        if (o.getFilledQuantity() == null) o.setFilledQuantity(0);
        if (o.getRemainingQuantity() == null) o.setRemainingQuantity(o.getQuantity());
    }

    private String businessChecks(SessionOrder o, SessionParticipation p) {
        if (o.getSide() == OrderSide.BUY) {
            double estimate = (o.getPrice() != null ? o.getPrice() : 0.0) * o.getQuantity();
            if (p.getCashActuel() < estimate) {
                return String.format("Fonds insuffisants (besoin: %.2f, disponible: %.2f)",
                        estimate, p.getCashActuel());
            }
        } else {
            double held = positionRepository
                    .findBySessionIdAndUserIdAndSymbol(o.getSession().getId(), o.getUser().getId(), o.getSymbol())
                    .map(SessionPosition::getQuantity).orElse(0.0);
            if (held < o.getQuantity()) {
                return String.format("Position insuffisante (besoin: %d, détenu: %.0f)", o.getQuantity(), held);
            }
        }
        return null;
    }

    private SessionOrder reject(SessionOrder o, String reason) {
        o.setStatus(OrderStatus.REJECTED);
        o.setRejectionReason(reason);
        if (o.getOrderTime() == null) o.setOrderTime(LocalDateTime.now());
        if (o.getFilledQuantity() == null) o.setFilledQuantity(0);
        if (o.getRemainingQuantity() == null && o.getQuantity() != null) o.setRemainingQuantity(o.getQuantity());
        return orderRepository.save(o);
    }

    @Transactional
    public void applyExecution(Long orderId, OrderSide side, int qty, double px) {
        SessionOrder o = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Ordre introuvable: " + orderId));
        if (!o.isExecutable()) return;

        int exec = Math.min(qty, o.getRemainingQuantity());
        if (exec <= 0) return;

        double cashDelta = px * exec;
        SessionParticipation p = participationService.getParticipation(o.getSession().getId(), o.getUser().getId());

        if (side == OrderSide.BUY) {
            p.setCashActuel(p.getCashActuel() - cashDelta);
            SessionPosition pos = positionRepository
                    .findBySessionIdAndUserIdAndSymbol(o.getSession().getId(), o.getUser().getId(), o.getSymbol())
                    .orElseGet(() -> SessionPosition.builder()
                            .session(o.getSession()).user(o.getUser()).symbol(o.getSymbol())
                            .quantity(0.0).avgPrice(0.0).build());
            double oldQ = pos.getQuantity();
            double newQ = oldQ + exec;
            double newAvg = (oldQ * pos.getAvgPrice() + cashDelta) / newQ;
            pos.setQuantity(newQ);
            pos.setAvgPrice(newAvg);
            positionRepository.save(pos);
        } else {
            p.setCashActuel(p.getCashActuel() + cashDelta);
            SessionPosition pos = positionRepository
                    .findBySessionIdAndUserIdAndSymbol(o.getSession().getId(), o.getUser().getId(), o.getSymbol())
                    .orElseThrow(() -> new RuntimeException("Position introuvable"));
            double newQ = pos.getQuantity() - exec;
            if (newQ < -0.001) throw new RuntimeException("Vente > position");
            pos.setQuantity(Math.max(0, newQ));
            double realized = (px - pos.getAvgPrice()) * exec;
            pos.setRealizedPnL((pos.getRealizedPnL() != null ? pos.getRealizedPnL() : 0.0) + realized);
            positionRepository.save(pos);
        }

        o.setFilledQuantity(o.getFilledQuantity() + exec);
        o.setRemainingQuantity(o.getRemainingQuantity() - exec);
        o.setExecutionPrice(px);
        o.setExecutionTime(LocalDateTime.now());
        if (o.getRemainingQuantity() <= 0) o.setStatus(OrderStatus.EXECUTED);

        orderRepository.save(o);
        participationService.incrementOrderCount(o.getSession().getId(), o.getUser().getId(), cashDelta);
    }

    @Transactional
    public SessionOrder executeOrder(Long orderId) {
        SessionOrder o = getOrderById(orderId);
        if (o.getStatus() != OrderStatus.PENDING) throw new RuntimeException("Ordre non exécutable manuellement");
        applyExecution(orderId, o.getSide(), o.getRemainingQuantity(), o.getPrice());
        return getOrderById(orderId);
    }

    @Transactional
    public SessionOrder cancelOrder(Long orderId) {
        SessionOrder o = getOrderById(orderId);
        if (o.getStatus() != OrderStatus.PENDING) throw new RuntimeException("Seuls les ordres PENDING peuvent être annulés");
        o.setStatus(OrderStatus.CANCELLED);
        o.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(o);
    }

    public SessionOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Ordre non trouvé"));
    }

    public List<SessionOrder> getSessionOrders(Long sessionId) {
        return orderRepository.findBySession_IdOrderByOrderTimeDesc(sessionId);
    }

    public List<SessionOrder> getUserOrders(Long sessionId, Long userId) {
        return orderRepository.findBySession_IdAndUser_IdOrderByOrderTimeDesc(sessionId, userId);
    }

    public List<SessionOrder> getActivityFeed(Long sessionId) {
        return orderRepository.getRecentExecutedOrders(sessionId);
    }

    public Double getTotalVolume(Long sessionId) {
        Double v = orderRepository.calculateTotalVolume(sessionId);
        return v != null ? v : 0.0;
    }

    public List<SessionOrder> getActiveOrders(Long sessionId) {
        return orderRepository.findBySession_IdAndStatusOrderByOrderTimeDesc(sessionId, OrderStatus.PENDING);
    }

    public List<SessionOrder> getUserActiveOrders(Long sessionId, Long userId) {
        return orderRepository.findBySession_IdAndUser_IdAndStatusOrderByOrderTimeDesc(
                sessionId, userId, OrderStatus.PENDING);
    }

    public long countOrdersByStatus(Long sessionId, OrderStatus status) {
        return orderRepository.countBySession_IdAndStatus(sessionId, status);
    }
}

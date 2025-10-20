// tn.esprit.examen.nomPrenomClasseExamen.services.SessionOrderService

package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.*;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.SessionOrderRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public SessionOrder placeOrder(SessionOrder order) {
        log.info("📝 Ordre: {} {} {} @ {}€", order.getSide(), order.getQuantity(), order.getSymbol(), order.getPrice());

        TradingSession session = sessionService.getSessionById(order.getSession().getId());
        if (session.getStatus() != SessionStatus.OPEN) {
            throw new RuntimeException("Marché fermé");
        }

        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        SessionParticipation participation = participationService.getParticipation(session.getId(), user.getId());

        if (order.getSide() == OrderSide.BUY) {
            double totalCost = order.getPrice() * order.getQuantity();
            if (participation.getCashActuel() < totalCost) {
                order.setStatus(OrderStatus.REJECTED);
                order.setRejectionReason("Fonds insuffisants");
                return orderRepository.save(order);
            }
        }

        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING);
        }

        if (order.getOrderTime() == null) {
            order.setOrderTime(LocalDateTime.now());
        }

        SessionOrder saved = orderRepository.save(order);

        if (order.getType() == OrderType.MARKET) {
            return executeOrder(saved.getId());
        }

        return saved;
    }

    @Transactional
    public SessionOrder executeOrder(Long orderId) {
        log.info("⚡ Exécution ordre: {}", orderId);

        SessionOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Ordre ne peut être exécuté");
        }

        SessionParticipation participation = participationService
                .getParticipation(order.getSession().getId(), order.getUser().getId());

        double amount = order.getPrice() * order.getQuantity();

        if (order.getSide() == OrderSide.BUY) {
            participation.setCashActuel(participation.getCashActuel() - amount);
        } else {
            participation.setCashActuel(participation.getCashActuel() + amount);
        }

        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutionTime(LocalDateTime.now());
        order.setExecutionPrice(order.getPrice());

        SessionOrder executed = orderRepository.save(order);

        participationService.incrementOrderCount(order.getSession().getId(), order.getUser().getId(), amount);

        return executed;
    }

    @Transactional
    public SessionOrder cancelOrder(Long orderId) {
        SessionOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Seuls les ordres en attente peuvent être annulés");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public SessionOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Ordre non trouvé"));
    }

    public List<SessionOrder> getSessionOrders(Long sessionId) {
        return orderRepository.findBySessionIdOrderByOrderTimeDesc(sessionId);
    }

    public List<SessionOrder> getUserOrders(Long sessionId, Long userId) {
        return orderRepository.findBySessionIdAndUserIdOrderByOrderTimeDesc(sessionId, userId);
    }

    public List<SessionOrder> getActivityFeed(Long sessionId) {
        return orderRepository.getRecentExecutedOrders(sessionId);
    }

    public Double getTotalVolume(Long sessionId) {
        Double volume = orderRepository.calculateTotalVolume(sessionId);
        return volume != null ? volume : 0.0;
    }
}
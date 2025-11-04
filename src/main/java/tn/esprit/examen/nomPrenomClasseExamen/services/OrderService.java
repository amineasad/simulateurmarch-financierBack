package tn.esprit.examen.nomPrenomClasseExamen.services;

import tn.esprit.examen.nomPrenomClasseExamen.DTO.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.*;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.UserTradingLimits;
import tn.esprit.examen.nomPrenomClasseExamen.entities.UserMarketControl;
import tn.esprit.examen.nomPrenomClasseExamen.services.userbook.UserBookService;
import tn.esprit.examen.nomPrenomClasseExamen.events.DomainEvent;
import tn.esprit.examen.nomPrenomClasseExamen.events.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion des ordres avec transactions ACID
 * - Isolation READ_COMMITTED pour éviter les dirty reads
 * - Propagation REQUIRED pour garantir l'atomicité
 * - Publication d'événements post-commit exactly-once
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final AssetRepository assetRepository;
    private final EventPublisher eventPublisher;
    private final PortfolioSettlementService settlementService;
    private final UserTradingLimitsRepository userTradingLimitsRepository;
    private final UserMarketControlRepository userMarketControlRepository;
    private final UserBookService userBookService;
    private final Map<Long, OrderBook> orderBooks = new ConcurrentHashMap<>();

    /**
     * Place un ordre avec validation et réservation atomiques
     * READ_COMMITTED: évite les dirty reads, acceptable pour notre moteur de matching
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public OrderView placeOrder(CreateOrderDTO dto, Long userId) {
        log.info("📝 Placement ordre: {} {} {} @ {}€", dto.OrderSide(), dto.quantity(), dto.Id(), dto.price());
        
        // Validation des données
        if (!dto.isValid()) {
            throw new IllegalArgumentException("Données d'ordre invalides");
        }

        // Récupération de l'actif
        Asset asset = assetRepository.findById(dto.Id())
                .orElseThrow(() -> new IllegalArgumentException("Actif non trouvé"));

        // Contrôles HALT ciblé
        if (isUserHalted(userId, dto.Id())) {
            return rejectOrder(userId, asset, dto, "Utilisateur HALTED sur cet actif");
        }

        // Validation des limites utilisateur
        String limitViolation = checkUserLimits(userId, dto.Id(), dto.quantity(), dto.price());
        if (limitViolation != null) {
            return rejectOrder(userId, asset, dto, limitViolation);
        }

        // Validation des fonds/quantités
        if (dto.OrderSide() == OrderSide.BUY) {
            if (!settlementService.validateBuyOrder(userId, dto.price(), dto.quantity())) {
                return rejectOrder(userId, asset, dto, "Fonds insuffisants");
            }
        } else {
            if (!settlementService.validateSellOrder(userId, dto.Id(), dto.quantity())) {
                return rejectOrder(userId, asset, dto, "Quantité insuffisante à vendre");
            }
        }

        // Création de l'ordre
        Order order = createOrder(dto, userId, asset);
        orderRepository.save(order);

        // Réservation des fonds/positions
        if (dto.OrderSide() == OrderSide.BUY) {
            settlementService.reserveBuyOrder(userId, dto.price(), dto.quantity());
        } else {
            settlementService.reserveSellOrder(userId, dto.Id(), dto.quantity());
        }

        // Mise en carnet et tentative de matching
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        OrderBook orderBook = orderBooks.computeIfAbsent(dto.Id(), id -> new OrderBook());
        orderBook.add(order);
        userBookService.onOrderAddedOrUpdated(order);

        // Tentative de matching
        matchOrders(dto.Id());

        // Publication d'événement post-commit
        eventPublisher.publishAfterCommit(
            DomainEvent.of("ORDER_STATUS", OrderView.from(order), userId.toString())
        );

        return OrderView.from(order);
    }

    /**
     * Annule un ordre avec libération des réservations
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void cancelOrder(Long orderId, Long userId) {
        log.info("❌ Annulation ordre: {} par utilisateur {}", orderId, userId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordre non trouvé"));

        if (!order.getUserId().equals(userId) || order.getStatus() != OrderStatus.PENDING) {
            log.warn("Impossible d'annuler l'ordre {}: propriétaire={}, statut={}", 
                    orderId, order.getUserId().equals(userId), order.getStatus());
            return;
        }

        // Retirer du carnet
        OrderBook orderBook = orderBooks.get(order.getAsset().getId());
        if (orderBook != null) {
            orderBook.remove(order);
        }

        // Libérer les réservations
        settlementService.cancelOrder(order, order.getRemainingQuantity());

        // Marquer comme annulé
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Publication d'événement post-commit
        eventPublisher.publishAfterCommit(
            DomainEvent.of("ORDER_STATUS", OrderView.from(order), userId.toString())
        );

        // Mise à jour du carnet
        pushOrderBookSnapshot(order.getAsset().getId());
    }

    /**
     * Matching des ordres avec règles price-time
     * Exécute les trades et met à jour les portefeuilles atomiquement
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void matchOrders(Long assetId) {
        OrderBook orderBook = orderBooks.get(assetId);
        if (orderBook == null || orderBook.isEmpty()) return;

        log.debug("🔄 Matching pour asset {}", assetId);

        while (true) {
            Optional<Order> bestBuyOpt = orderBook.peekBest(OrderSide.BUY);
            Optional<Order> bestSellOpt = orderBook.peekBest(OrderSide.SELL);

            if (bestBuyOpt.isEmpty() || bestSellOpt.isEmpty()) break;

            Order bestBuy = bestBuyOpt.get();
            Order bestSell = bestSellOpt.get();

            // Vérifier si les ordres peuvent matcher
            if (!orderBook.canMatch(bestBuy) && !orderBook.canMatch(bestSell)) {
                break;
            }

            // Déterminer le prix d'exécution
            BigDecimal executionPrice = orderBook.getExecutionPrice(bestBuy, bestSell);
            if (executionPrice == null) break;

            // Retirer du carnet
            orderBook.pollBest(OrderSide.BUY);
            orderBook.pollBest(OrderSide.SELL);

            int quantity = Math.min(bestBuy.getRemainingQuantity(), bestSell.getRemainingQuantity());

            // Créer le trade
            Trade trade = createTrade(assetId, bestBuy, bestSell, executionPrice, quantity);
            tradeRepository.save(trade);

            // Règlement des portefeuilles
            settlementService.settleBuyOrder(bestBuy, quantity, executionPrice);
            settlementService.settleSellOrder(bestSell, quantity, executionPrice);

            // Mise à jour des ordres
            updateOrderAfterFill(bestBuy, quantity);
            updateOrderAfterFill(bestSell, quantity);

            // Mettre à jour les carnets utilisateurs
            userBookService.onOrderAddedOrUpdated(bestBuy);
            userBookService.onOrderAddedOrUpdated(bestSell);

            // Publication d'événements post-commit
            eventPublisher.publishAfterCommit(
                DomainEvent.of("TRANSACTION", TradeView.from(trade), bestBuy.getUserId().toString())
            );
            eventPublisher.publishAfterCommit(
                DomainEvent.of("TRANSACTION", TradeView.from(trade), bestSell.getUserId().toString())
            );

            // Réinsérer l'ordre s'il reste du volume
            if (bestBuy.getRemainingQuantity() > 0) {
                orderBook.add(bestBuy);
            }
            if (bestSell.getRemainingQuantity() > 0) {
                orderBook.add(bestSell);
            }
        }

        // Publication du carnet d'ordres
        pushOrderBookSnapshot(assetId);
    }

    // Méthodes utilitaires privées

    private Order createOrder(CreateOrderDTO dto, Long userId, Asset asset) {
        Order order = new Order();
        order.setUserId(userId);
        order.setAsset(asset);
        order.setSide(dto.OrderSide());
        order.setType(dto.type());
        order.setStatus(OrderStatus.NEW);
        order.setPrice(dto.price());
        order.setQuantity(dto.quantity());
        order.setRemainingQuantity(dto.quantity());
        order.setCreatedAt(Instant.now());
        return order;
    }

    private OrderView rejectOrder(Long userId, Asset asset, CreateOrderDTO dto, String reason) {
        Order order = createOrder(dto, userId, asset);
        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);

        OrderView orderView = OrderView.from(order).withError(reason);
        
        // Publication immédiate pour les rejets
        eventPublisher.publishImmediately(
            DomainEvent.of("ORDER_STATUS", orderView, userId.toString())
        );

        return orderView;
    }

    private Trade createTrade(Long assetId, Order buyOrder, Order sellOrder, BigDecimal price, int quantity) {
        Trade trade = new Trade();
        trade.setAssetId(assetId);
        trade.setBuyOrderId(buyOrder.getId());
        trade.setSellOrderId(sellOrder.getId());
        trade.setPrice(price);
        trade.setQuantity(quantity);
        trade.setExecutedAt(Instant.now());
        return trade;
    }

    private void updateOrderAfterFill(Order order, int filledQuantity) {
        order.setRemainingQuantity(order.getRemainingQuantity() - filledQuantity);

        if (order.getRemainingQuantity() == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }

        orderRepository.save(order);

        // Publication d'événement post-commit
        eventPublisher.publishAfterCommit(
            DomainEvent.of("ORDER_STATUS", OrderView.from(order), order.getUserId().toString())
        );
    }

    private boolean isUserHalted(Long userId, Long assetId) {
        return userMarketControlRepository.findByUserIdAndAssetId(userId, assetId)
                .map(ctrl -> ctrl.getState() == UserMarketControl.State.HALTED_USER)
                .orElse(false);
    }

    private String checkUserLimits(Long userId, Long assetId, int qty, BigDecimal price) {
        var limits = userTradingLimitsRepository.findEffectiveLimits(userId, assetId);
        if (limits.isEmpty()) return null;
        UserTradingLimits effective = limits.get(0); // asset-specific prioritaire (requête ordonnée)
        if (effective.isBlocked()) return "Utilisateur bloqué";
        if (effective.getMaxOrderSize() != null && qty > effective.getMaxOrderSize()) return "Taille d'ordre maximale dépassée";

        if (effective.getMaxOpenOrders() != null) {
            int openOrders = (int) orderRepository.findByUserId(userId).stream()
                    .filter(o -> o.getAsset().getId().equals(assetId) && (o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.PARTIALLY_FILLED))
                    .count();
            if (openOrders >= effective.getMaxOpenOrders()) return "Nombre d'ordres ouverts dépassé";
        }

        if (effective.getMaxDailyNotional() != null && price != null) {
            BigDecimal notionalEstimate = price.multiply(BigDecimal.valueOf(qty));
            // Simplification: on ne calcule pas le cumulé journalier ici, à raffiner avec une table d'agrégats
            if (notionalEstimate.compareTo(effective.getMaxDailyNotional()) > 0) return "Notional journalier dépassé (estimation)";
        }
        return null;
    }

    private void pushOrderBookSnapshot(Long assetId) {
        OrderBook orderBook = orderBooks.get(assetId);
        if (orderBook == null) return;

        List<OrderBookLevel> bids = orderBook.getBids().stream()
                .map(order -> new OrderBookLevel(order.getPrice(), order.getRemainingQuantity()))
                .toList();

        List<OrderBookLevel> asks = orderBook.getAsks().stream()
                .map(order -> new OrderBookLevel(order.getPrice(), order.getRemainingQuantity()))
                .toList();

        BigDecimal bestBid = orderBook.getBestBidPrice().orElse(null);
        BigDecimal bestAsk = orderBook.getBestAskPrice().orElse(null);

        OrderBookSnapshot snapshot = new OrderBookSnapshot(bids, asks, bestBid, bestAsk);
        
        // Publication d'événement post-commit
        eventPublisher.publishAfterCommit(
            DomainEvent.of("ORDERBOOK", snapshot, null, assetId.toString())
        );
    }

    // Méthodes de consultation

    public List<OrderView> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderView::from)
                .toList();
    }

    public List<TradeView> getUserTrades(Long userId) {
        return tradeRepository.findByBuyOrderIdOrSellOrderId(userId, userId).stream()
                .map(TradeView::from)
                .toList();
    }

    public OrderBookSnapshot getOrderBookSnapshot(Long assetId) {
        OrderBook orderBook = orderBooks.get(assetId);
        if (orderBook == null) {
            return new OrderBookSnapshot(List.of(), List.of(), null, null);
        }

        List<OrderBookLevel> bids = orderBook.getBids().stream()
                .map(order -> new OrderBookLevel(order.getPrice(), order.getRemainingQuantity()))
                .toList();

        List<OrderBookLevel> asks = orderBook.getAsks().stream()
                .map(order -> new OrderBookLevel(order.getPrice(), order.getRemainingQuantity()))
                .toList();

        BigDecimal bestBid = orderBook.getBestBidPrice().orElse(null);
        BigDecimal bestAsk = orderBook.getBestAskPrice().orElse(null);

        return new OrderBookSnapshot(bids, asks, bestBid, bestAsk);
    }
}
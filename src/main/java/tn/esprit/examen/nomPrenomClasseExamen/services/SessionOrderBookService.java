// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/services/SessionOrderBookService.java
package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderType;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionOrder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionOrderBookService {

    private final SessionOrderService orderService;

    /**
     * Conteneur d'un carnet pour (sessionId, symbol)
     */
    private static class Book {
        final PriorityBlockingQueue<SessionOrder> bids;
        final PriorityBlockingQueue<SessionOrder> asks;
        double lastPrice;

        Book(Comparator<SessionOrder> bidCmp, Comparator<SessionOrder> askCmp) {
            this.bids = new PriorityBlockingQueue<>(64, bidCmp);
            this.asks = new PriorityBlockingQueue<>(64, askCmp);
            this.lastPrice = 100.0; // Prix initial par défaut
        }
    }

    // Carnets en mémoire : key = "sessionId#SYMBOL"
    private final Map<String, Book> books = new ConcurrentHashMap<>();

    // Séquence globale pour l'ordre de priorité temporelle
    private final AtomicLong globalSequence = new AtomicLong(0);

    // Comparateurs pour le matching prix-temps
    private final Comparator<SessionOrder> bidComparator = Comparator
            .comparing(SessionOrder::getPrice).reversed()  // Prix décroissant
            .thenComparing(SessionOrder::getSequence);     // Puis ancienneté

    private final Comparator<SessionOrder> askComparator = Comparator
            .comparing(SessionOrder::getPrice)             // Prix croissant
            .thenComparing(SessionOrder::getSequence);     // Puis ancienneté

    /**
     * Génère une clé unique pour le carnet
     */
    private String bookKey(Long sessionId, String symbol) {
        return sessionId + "#" + symbol.toUpperCase();
    }

    /**
     * Récupère ou crée un carnet pour (sessionId, symbol)
     */
    private Book getBook(Long sessionId, String symbol) {
        return books.computeIfAbsent(
                bookKey(sessionId, symbol),
                k -> new Book(bidComparator, askComparator)
        );
    }

    /**
     * Soumet un ordre au carnet (déjà validé et persisté avec status=PENDING)
     */
    public synchronized void submit(SessionOrder order) {
        if (order == null || !order.isExecutable()) {
            log.warn("⚠️ Ordre non exécutable ignoré: {}", order);
            return;
        }

        // Assigner une séquence pour le matching temporel
        order.setSequence(globalSequence.incrementAndGet());

        Book book = getBook(order.getSession().getId(), order.getSymbol());

        log.info("📥 Soumission ordre #{} : {} {} {} @ {}",
                order.getId(), order.getSide(), order.getQuantity(),
                order.getSymbol(), order.getPrice());

        if (order.getType() == OrderType.MARKET) {
            matchMarketOrder(order, book);
        } else if (order.getType() == OrderType.LIMIT) {
            addLimitOrder(order, book);
            crossOrders(book, order.getSession().getId(), order.getSymbol());
        } else {
            log.warn("⚠️ Type d'ordre non géré: {}", order.getType());
        }
    }

    /**
     * Ajoute un ordre LIMIT au carnet
     */
    private void addLimitOrder(SessionOrder order, Book book) {
        if (order.getSide() == OrderSide.BUY) {
            book.bids.add(order);
            log.debug("📗 Ajouté au BID: {} @ {}", order.getQuantity(), order.getPrice());
        } else {
            book.asks.add(order);
            log.debug("📕 Ajouté au ASK: {} @ {}", order.getQuantity(), order.getPrice());
        }
    }

    /**
     * Fait croiser les ordres BID et ASK compatibles (matching)
     */
    private void crossOrders(Book book, Long sessionId, String symbol) {
        while (!book.bids.isEmpty() && !book.asks.isEmpty()) {
            SessionOrder bid = book.bids.peek();
            SessionOrder ask = book.asks.peek();

            // Vérifier que les ordres sont toujours valides
            if (bid == null || ask == null ||
                    bid.getRemainingQuantity() <= 0 ||
                    ask.getRemainingQuantity() <= 0) {
                cleanupOrders(book);
                continue;
            }

            // Pas de croisement de prix
            if (bid.getPrice() < ask.getPrice()) {
                break;
            }

            // Match trouvé !
            int tradeQuantity = Math.min(
                    bid.getRemainingQuantity(),
                    ask.getRemainingQuantity()
            );

            // Prix d'exécution = prix de l'ordre passif (ASK dans ce cas)
            double executionPrice = ask.getPrice();

            executeTrade(bid, ask, tradeQuantity, executionPrice);
            book.lastPrice = executionPrice;

            log.info("💥 TRADE: {} {} @ {} (BID#{} vs ASK#{})",
                    tradeQuantity, symbol, executionPrice, bid.getId(), ask.getId());

            // Retirer les ordres complètement exécutés
            if (bid.getRemainingQuantity() <= 0) {
                book.bids.poll();
            }
            if (ask.getRemainingQuantity() <= 0) {
                book.asks.poll();
            }
        }
    }

    /**
     * Exécute un ordre MARKET contre le meilleur côté opposé
     */
    private void matchMarketOrder(SessionOrder marketOrder, Book book) {
        if (marketOrder.getSide() == OrderSide.BUY) {
            // Acheter contre les ASKs
            while (marketOrder.getRemainingQuantity() > 0 && !book.asks.isEmpty()) {
                SessionOrder ask = book.asks.peek();

                if (ask == null || ask.getRemainingQuantity() <= 0) {
                    book.asks.poll();
                    continue;
                }

                int tradeQuantity = Math.min(
                        marketOrder.getRemainingQuantity(),
                        ask.getRemainingQuantity()
                );
                double executionPrice = ask.getPrice();

                executeTrade(marketOrder, ask, tradeQuantity, executionPrice);
                book.lastPrice = executionPrice;

                if (ask.getRemainingQuantity() <= 0) {
                    book.asks.poll();
                }
            }
        } else {
            // Vendre contre les BIDs
            while (marketOrder.getRemainingQuantity() > 0 && !book.bids.isEmpty()) {
                SessionOrder bid = book.bids.peek();

                if (bid == null || bid.getRemainingQuantity() <= 0) {
                    book.bids.poll();
                    continue;
                }

                int tradeQuantity = Math.min(
                        marketOrder.getRemainingQuantity(),
                        bid.getRemainingQuantity()
                );
                double executionPrice = bid.getPrice();

                executeTrade(bid, marketOrder, tradeQuantity, executionPrice);
                book.lastPrice = executionPrice;

                if (bid.getRemainingQuantity() <= 0) {
                    book.bids.poll();
                }
            }
        }

        // Si reste une partie non exécutée (pas de contrepartie)
        if (marketOrder.getRemainingQuantity() > 0) {
            double fallbackPrice = book.lastPrice > 0 ? book.lastPrice :
                    (marketOrder.getPrice() != null ? marketOrder.getPrice() : 100.0);

            log.warn("⚠️ MARKET order #{} partiellement exécuté. Reste: {} unités. Fallback @ {}",
                    marketOrder.getId(), marketOrder.getRemainingQuantity(), fallbackPrice);

            orderService.applyExecution(
                    marketOrder.getId(),
                    marketOrder.getSide(),
                    marketOrder.getRemainingQuantity(),
                    fallbackPrice
            );

            marketOrder.setRemainingQuantity(0);
        }
    }

    /**
     * Exécute un trade entre deux ordres
     */
    private void executeTrade(SessionOrder buyOrder, SessionOrder sellOrder,
                              int quantity, double price) {
        orderService.applyExecution(buyOrder.getId(), OrderSide.BUY, quantity, price);
        orderService.applyExecution(sellOrder.getId(), OrderSide.SELL, quantity, price);
    }

    /**
     * Nettoie les ordres invalides du carnet
     */
    private void cleanupOrders(Book book) {
        book.bids.removeIf(o -> o == null || o.getRemainingQuantity() <= 0 || !o.isExecutable());
        book.asks.removeIf(o -> o == null || o.getRemainingQuantity() <= 0 || !o.isExecutable());
    }

    /**
     * Retourne un snapshot du carnet pour l'UI (agrégé par niveau de prix)
     */
    public synchronized OrderBookDepth depth(Long sessionId, String symbol, int levels) {
        Book book = getBook(sessionId, symbol);

        OrderBookDepth depth = new OrderBookDepth();
        depth.lastPrice = book.lastPrice;

        // Agrégation par prix
        Map<Double, Integer> aggregatedBids = new TreeMap<>(Comparator.reverseOrder());
        Map<Double, Integer> aggregatedAsks = new TreeMap<>();

        // Agréger les BIDs
        for (SessionOrder order : book.bids) {
            if (order != null && order.getRemainingQuantity() > 0) {
                aggregatedBids.merge(
                        order.getPrice(),
                        order.getRemainingQuantity(),
                        Integer::sum
                );
            }
        }

        // Agréger les ASKs
        for (SessionOrder order : book.asks) {
            if (order != null && order.getRemainingQuantity() > 0) {
                aggregatedAsks.merge(
                        order.getPrice(),
                        order.getRemainingQuantity(),
                        Integer::sum
                );
            }
        }

        // Limiter au nombre de niveaux demandés
        int maxLevels = Math.max(1, Math.min(levels, 50));

        aggregatedBids.entrySet().stream()
                .limit(maxLevels)
                .forEach(entry -> {
                    OrderBookDepth.Level level = new OrderBookDepth.Level();
                    level.price = entry.getKey();
                    level.quantity = entry.getValue(); // ✅ "quantity" pas "qty"
                    depth.bids.add(level);
                });

        aggregatedAsks.entrySet().stream()
                .limit(maxLevels)
                .forEach(entry -> {
                    OrderBookDepth.Level level = new OrderBookDepth.Level();
                    level.price = entry.getKey();
                    level.quantity = entry.getValue(); // ✅ "quantity" pas "qty"
                    depth.asks.add(level);
                });

        return depth;
    }

    /**
     * DTO pour le carnet d'ordres
     */
    public static class OrderBookDepth {
        public static class Level {
            public double price;
            public int quantity; // ✅ Cohérent avec le front
        }

        public List<Level> bids = new ArrayList<>(); // Triés desc
        public List<Level> asks = new ArrayList<>(); // Triés asc
        public double lastPrice;
    }

    /**
     * Méthode utilitaire pour voir tous les carnets actifs (debug)
     */
    public Map<String, OrderBookDepth> getAllActiveBooks() {
        Map<String, OrderBookDepth> result = new HashMap<>();

        books.forEach((key, book) -> {
            String[] parts = key.split("#");
            if (parts.length == 2) {
                Long sessionId = Long.parseLong(parts[0]);
                String symbol = parts[1];
                result.put(key, depth(sessionId, symbol, 10));
            }
        });

        return result;
    }
}
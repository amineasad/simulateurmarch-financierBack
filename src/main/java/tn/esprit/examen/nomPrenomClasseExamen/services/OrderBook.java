package tn.esprit.examen.nomPrenomClasseExamen.services;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Order;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderType;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Carnet d'ordres avec PriorityQueues price-time
 * - BUY: prix décroissant puis antériorité (FIFO à prix égal)
 * - SELL: prix croissant puis antériorité (FIFO à prix égal)
 * - Support MARKET vs LIMIT avec règles d'exécution appropriées
 */
@Component
public class OrderBook {

    // BUY orders: meilleur prix (plus élevé) en tête, puis antériorité
    private final PriorityQueue<Order> bids = new PriorityQueue<>(
            Comparator.<Order, BigDecimal>comparing(
                    o -> o.getType() == OrderType.MARKET ? BigDecimal.ZERO : o.getPrice(),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed() // Prix décroissant pour BUY
            .thenComparing(Order::getCreatedAt) // FIFO à prix égal
    );

    // SELL orders: meilleur prix (plus bas) en tête, puis antériorité
    private final PriorityQueue<Order> asks = new PriorityQueue<>(
            Comparator.<Order, BigDecimal>comparing(
                    o -> o.getType() == OrderType.MARKET ? BigDecimal.valueOf(Long.MAX_VALUE) : o.getPrice(),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ) // Prix croissant pour SELL
            .thenComparing(Order::getCreatedAt) // FIFO à prix égal
    );

    /**
     * Ajoute un ordre au carnet
     * Les ordres MARKET sont traités spécialement pour le matching
     */
    public synchronized void add(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.add(order);
        } else {
            asks.add(order);
        }
    }

    /**
     * Retire et retourne le meilleur ordre du côté demandé
     * Respecte la priorité price-time
     */
    public synchronized Order pollBest(OrderSide side) {
        return side == OrderSide.BUY ? bids.poll() : asks.poll();
    }

    /**
     * Regarde le meilleur ordre sans le retirer
     */
    public synchronized Optional<Order> peekBest(OrderSide side) {
        return Optional.ofNullable(side == OrderSide.BUY ? bids.peek() : asks.peek());
    }

    /**
     * Vérifie si le carnet est vide
     */
    public synchronized boolean isEmpty() {
        return bids.isEmpty() && asks.isEmpty();
    }

    /**
     * Retourne le nombre total d'ordres
     */
    public synchronized int size() {
        return bids.size() + asks.size();
    }

    /**
     * Vide le carnet
     */
    public synchronized void clear() {
        bids.clear();
        asks.clear();
    }

    /**
     * Retire un ordre spécifique du carnet
     * Utilisé pour les annulations
     */
    public synchronized boolean remove(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            return bids.remove(order);
        } else {
            return asks.remove(order);
        }
    }

    /**
     * Vérifie si un ordre peut matcher avec le meilleur ordre opposé
     * Règle: BUY peut matcher si son prix >= meilleur ASK
     *        SELL peut matcher si son prix <= meilleur BID
     */
    public synchronized boolean canMatch(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            Optional<Order> bestAsk = peekBest(OrderSide.SELL);
            if (bestAsk.isEmpty()) return false;
            
            Order ask = bestAsk.get();
            // BUY MARKET peut toujours matcher
            if (order.getType() == OrderType.MARKET) return true;
            // BUY LIMIT matche si prix >= meilleur ASK
            return order.getPrice().compareTo(ask.getPrice()) >= 0;
            
        } else {
            Optional<Order> bestBid = peekBest(OrderSide.BUY);
            if (bestBid.isEmpty()) return false;
            
            Order bid = bestBid.get();
            // SELL MARKET peut toujours matcher
            if (order.getType() == OrderType.MARKET) return true;
            // SELL LIMIT matche si prix <= meilleur BID
            return order.getPrice().compareTo(bid.getPrice()) <= 0;
        }
    }

    /**
     * Détermine le prix d'exécution selon les règles du marché
     * - LIMIT vs LIMIT: prix de l'ordre le plus ancien
     * - MARKET vs LIMIT: prix de l'ordre LIMIT
     * - MARKET vs MARKET: prix de référence (à implémenter)
     */
    public synchronized BigDecimal getExecutionPrice(Order buyOrder, Order sellOrder) {
        // LIMIT vs LIMIT: prix de l'ordre le plus ancien
        if (buyOrder.getType() == OrderType.LIMIT && sellOrder.getType() == OrderType.LIMIT) {
            return buyOrder.getCreatedAt().isBefore(sellOrder.getCreatedAt()) 
                ? buyOrder.getPrice() 
                : sellOrder.getPrice();
        }
        
        // MARKET vs LIMIT: prix de l'ordre LIMIT
        if (buyOrder.getType() == OrderType.MARKET && sellOrder.getType() == OrderType.LIMIT) {
            return sellOrder.getPrice();
        }
        if (buyOrder.getType() == OrderType.LIMIT && sellOrder.getType() == OrderType.MARKET) {
            return buyOrder.getPrice();
        }
        
        // MARKET vs MARKET: prix de référence (à implémenter avec un service de prix)
        if (buyOrder.getType() == OrderType.MARKET && sellOrder.getType() == OrderType.MARKET) {
            // Dans un vrai système, on utiliserait un prix de référence externe
            return BigDecimal.valueOf(100.0); // Prix par défaut
        }
        
        return null; // Pas de croisement possible
    }

    /**
     * Retourne une copie des ordres BUY triés par priorité
     */
    public synchronized List<Order> getBids() {
        return new ArrayList<>(bids).stream()
                .sorted(Comparator.<Order, BigDecimal>comparing(
                        o -> o.getType() == OrderType.MARKET ? BigDecimal.ZERO : o.getPrice(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
                .thenComparing(Order::getCreatedAt))
                .collect(Collectors.toList());
    }

    /**
     * Retourne une copie des ordres SELL triés par priorité
     */
    public synchronized List<Order> getAsks() {
        return new ArrayList<>(asks).stream()
                .sorted(Comparator.<Order, BigDecimal>comparing(
                        o -> o.getType() == OrderType.MARKET ? BigDecimal.valueOf(Long.MAX_VALUE) : o.getPrice(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(Order::getCreatedAt))
                .collect(Collectors.toList());
    }

    /**
     * Retourne le meilleur prix BUY (best bid)
     */
    public synchronized Optional<BigDecimal> getBestBidPrice() {
        return peekBest(OrderSide.BUY).map(Order::getPrice);
    }

    /**
     * Retourne le meilleur prix SELL (best ask)
     */
    public synchronized Optional<BigDecimal> getBestAskPrice() {
        return peekBest(OrderSide.SELL).map(Order::getPrice);
    }

    /**
     * Retourne le spread (différence entre best ask et best bid)
     */
    public synchronized Optional<BigDecimal> getSpread() {
        Optional<BigDecimal> bestBid = getBestBidPrice();
        Optional<BigDecimal> bestAsk = getBestAskPrice();
        
        if (bestBid.isPresent() && bestAsk.isPresent()) {
            return Optional.of(bestAsk.get().subtract(bestBid.get()));
        }
        
        return Optional.empty();
    }
}

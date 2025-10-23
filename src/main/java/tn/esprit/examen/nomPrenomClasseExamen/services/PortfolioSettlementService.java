package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.*;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.PortfolioRepository;

import java.math.BigDecimal;

/**
 * Service de règlement portefeuille
 * Gère les opérations BUY/SELL/CANCEL avec atomicité
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSettlementService {
    
    private final PortfolioRepository portfolioRepository;
    
    /**
     * Règlement d'un ordre d'ACHAT
     * - Réduit la trésorerie réservée et disponible
     * - Augmente la position sur l'actif
     * - Gère le remboursement si exécution à meilleur prix (LIMIT)
     */
    @Transactional
    public void settleBuyOrder(Order order, int quantity, BigDecimal executionPrice) {
        Portfolio portfolio = portfolioRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Portefeuille non trouvé"));
        
        BigDecimal totalCost = executionPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal reservedCost = order.getPrice().multiply(BigDecimal.valueOf(quantity));
        
        log.debug("Règlement ACHAT: {} {} @ {}€ (réservé: {}€)", 
                quantity, order.getAsset().getSymbol(), executionPrice, order.getPrice());
        
        // Réduire la trésorerie réservée
        portfolio.setReservedCash(portfolio.getReservedCash().subtract(reservedCost));
        
        // Réduire la trésorerie disponible
        portfolio.setCash(portfolio.getCash().subtract(totalCost));
        
        // Augmenter la position
        portfolio.getPositions().merge(
                order.getAsset().getId(), 
                quantity, 
                Integer::sum
        );
        
        // Remboursement si exécution à meilleur prix (LIMIT)
        if (order.getType() == OrderType.LIMIT && executionPrice.compareTo(order.getPrice()) < 0) {
            BigDecimal refund = reservedCost.subtract(totalCost);
            portfolio.setCash(portfolio.getCash().add(refund));
            log.debug("Remboursement LIMIT: {}€", refund);
        }
        
        portfolioRepository.save(portfolio);
        log.debug("Portefeuille ACHAT mis à jour: cash={}, positions[{}]={}", 
                portfolio.getCash(), order.getAsset().getSymbol(), 
                portfolio.getPositions().get(order.getAsset().getId()));
    }
    
    /**
     * Règlement d'un ordre de VENTE
     * - Réduit la position réservée et disponible
     * - Augmente la trésorerie
     */
    @Transactional
    public void settleSellOrder(Order order, int quantity, BigDecimal executionPrice) {
        Portfolio portfolio = portfolioRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Portefeuille non trouvé"));
        
        BigDecimal totalRevenue = executionPrice.multiply(BigDecimal.valueOf(quantity));
        
        log.debug("Règlement VENTE: {} {} @ {}€", 
                quantity, order.getAsset().getSymbol(), executionPrice);
        
        // Réduire la position réservée
        portfolio.getReserved().merge(
                order.getAsset().getId(), 
                -quantity, 
                Integer::sum
        );
        
        // Nettoyer les réservations nulles
        if (portfolio.getReserved().getOrDefault(order.getAsset().getId(), 0) <= 0) {
            portfolio.getReserved().remove(order.getAsset().getId());
        }
        
        // Réduire la position disponible
        portfolio.getPositions().merge(
                order.getAsset().getId(), 
                -quantity, 
                Integer::sum
        );
        
        // Augmenter la trésorerie
        portfolio.setCash(portfolio.getCash().add(totalRevenue));
        
        portfolioRepository.save(portfolio);
        log.debug("Portefeuille VENTE mis à jour: cash={}, positions[{}]={}", 
                portfolio.getCash(), order.getAsset().getSymbol(), 
                portfolio.getPositions().get(order.getAsset().getId()));
    }
    
    /**
     * Annulation d'un ordre - libération des réservations
     * - BUY: libère la trésorerie réservée
     * - SELL: libère la position réservée
     */
    @Transactional
    public void cancelOrder(Order order, int remainingQuantity) {
        Portfolio portfolio = portfolioRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Portefeuille non trouvé"));
        
        log.debug("Annulation ordre: {} {} (reste: {})", 
                order.getSide(), order.getAsset().getSymbol(), remainingQuantity);
        
        if (order.getSide() == OrderSide.BUY) {
            // Libérer la trésorerie réservée
            BigDecimal reservedCost = order.getPrice().multiply(BigDecimal.valueOf(remainingQuantity));
            portfolio.setReservedCash(portfolio.getReservedCash().subtract(reservedCost));
            log.debug("Trésorerie réservée libérée: {}€", reservedCost);
            
        } else {
            // Libérer la position réservée
            portfolio.getReserved().merge(
                    order.getAsset().getId(), 
                    -remainingQuantity, 
                    Integer::sum
            );
            
            // Nettoyer les réservations nulles
            if (portfolio.getReserved().getOrDefault(order.getAsset().getId(), 0) <= 0) {
                portfolio.getReserved().remove(order.getAsset().getId());
            }
            log.debug("Position réservée libérée: {} {}", 
                    remainingQuantity, order.getAsset().getSymbol());
        }
        
        portfolioRepository.save(portfolio);
        log.debug("Portefeuille annulation mis à jour: cash={}, reservedCash={}", 
                portfolio.getCash(), portfolio.getReservedCash());
    }
    
    /**
     * Validation des fonds pour un ordre d'ACHAT
     * Vérifie que l'utilisateur a suffisamment de trésorerie disponible
     */
    public boolean validateBuyOrder(Long userId, BigDecimal price, int quantity) {
        Portfolio portfolio = portfolioRepository.findById(userId).orElse(null);
        if (portfolio == null) return false;
        
        BigDecimal requiredCash = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal availableCash = portfolio.getCash().subtract(portfolio.getReservedCash());
        
        boolean valid = availableCash.compareTo(requiredCash) >= 0;
        log.debug("Validation ACHAT: requis={}€, disponible={}€, valide={}", 
                requiredCash, availableCash, valid);
        
        return valid;
    }
    
    /**
     * Validation de la quantité pour un ordre de VENTE
     * Vérifie que l'utilisateur a suffisamment de position disponible
     */
    public boolean validateSellOrder(Long userId, Long assetId, int quantity) {
        Portfolio portfolio = portfolioRepository.findById(userId).orElse(null);
        if (portfolio == null) return false;
        
        int availableQuantity = portfolio.getPositions().getOrDefault(assetId, 0) - 
                               portfolio.getReserved().getOrDefault(assetId, 0);
        
        boolean valid = availableQuantity >= quantity;
        log.debug("Validation VENTE: requis={}, disponible={}, valide={}", 
                quantity, availableQuantity, valid);
        
        return valid;
    }
    
    /**
     * Réservation des fonds pour un ordre d'ACHAT
     * Réserve la trésorerie nécessaire avant mise en carnet
     */
    @Transactional
    public void reserveBuyOrder(Long userId, BigDecimal price, int quantity) {
        Portfolio portfolio = portfolioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Portefeuille non trouvé"));
        
        BigDecimal cost = price.multiply(BigDecimal.valueOf(quantity));
        portfolio.setReservedCash(portfolio.getReservedCash().add(cost));
        
        portfolioRepository.save(portfolio);
        log.debug("Trésorerie réservée: {}€ pour {} @ {}€", cost, quantity, price);
    }
    
    /**
     * Réservation de la position pour un ordre de VENTE
     * Réserve la quantité nécessaire avant mise en carnet
     */
    @Transactional
    public void reserveSellOrder(Long userId, Long assetId, int quantity) {
        Portfolio portfolio = portfolioRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Portefeuille non trouvé"));
        
        portfolio.getReserved().merge(assetId, quantity, Integer::sum);
        
        portfolioRepository.save(portfolio);
        log.debug("Position réservée: {} pour asset {}", quantity, assetId);
    }
}

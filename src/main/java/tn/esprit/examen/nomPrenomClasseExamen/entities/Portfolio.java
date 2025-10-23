package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "portfolios")
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cash = BigDecimal.ZERO;

    // Positions par actif (assetId -> qty)
    @ElementCollection
    @CollectionTable(name = "portfolio_positions", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "asset_id")
    @Column(name = "quantity")
    private Map<Long, Integer> positions = new HashMap<>();

    // Quantité réservée pour SELL (assetId -> qty)
    @ElementCollection
    @CollectionTable(name = "portfolio_reserved", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "asset_id")
    @Column(name = "quantity")
    private Map<Long, Integer> reserved = new HashMap<>();

    // Cash réservé pour BUY
    @Column(name = "reserved_cash", precision = 19, scale = 4)
    private BigDecimal reservedCash = BigDecimal.ZERO;

    // Constructeurs
    public Portfolio() {}

    public Portfolio(Long userId, BigDecimal cash) {
        this.userId = userId;
        this.cash = cash;
    }

    // Getters et Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public Map<Long, Integer> getPositions() {
        return positions;
    }

    public void setPositions(Map<Long, Integer> positions) {
        this.positions = positions;
    }

    public Map<Long, Integer> getReserved() {
        return reserved;
    }

    public void setReserved(Map<Long, Integer> reserved) {
        this.reserved = reserved;
    }

    public BigDecimal getReservedCash() {
        return reservedCash;
    }

    public void setReservedCash(BigDecimal reservedCash) {
        this.reservedCash = reservedCash;
    }
}

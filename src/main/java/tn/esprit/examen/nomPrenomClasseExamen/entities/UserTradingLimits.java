package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_trading_limits")
public class UserTradingLimits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private Long assetId; // null => limite globale

    @Column(precision = 19, scale = 4)
    private BigDecimal maxDailyNotional;

    @Column
    private Integer maxOrderSize;

    @Column
    private Integer maxOpenOrders;

    @Column(nullable = false)
    private boolean blocked = false;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public BigDecimal getMaxDailyNotional() { return maxDailyNotional; }
    public void setMaxDailyNotional(BigDecimal v) { this.maxDailyNotional = v; }
    public Integer getMaxOrderSize() { return maxOrderSize; }
    public void setMaxOrderSize(Integer v) { this.maxOrderSize = v; }
    public Integer getMaxOpenOrders() { return maxOpenOrders; }
    public void setMaxOpenOrders(Integer v) { this.maxOpenOrders = v; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}


// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/entities/SessionPosition.java
package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "session_positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_session_user_symbol",
                columnNames = {"session_id", "user_id", "symbol"}
        ),
        indexes = {
                @Index(name = "idx_session_positions_session", columnList = "session_id"),
                @Index(name = "idx_session_positions_user", columnList = "user_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = {"session", "user"})
public class SessionPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TradingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false)
    @Builder.Default
    private Double quantity = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double avgPrice = 0.0;

    // ✅ CORRECTION : Ajout de @Builder.Default
    @Column(nullable = false)
    @Builder.Default
    private Double unrealizedPnL = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double realizedPnL = 0.0;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // ✅ SÉCURITÉ : S'assurer que les PnL ne sont jamais null
        if (this.unrealizedPnL == null) this.unrealizedPnL = 0.0;
        if (this.realizedPnL == null) this.realizedPnL = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Utilitaires
    public double getMarketValue(double currentPrice) {
        return this.quantity * currentPrice;
    }

    public double getUnrealizedPnL(double currentPrice) {
        return (currentPrice - this.avgPrice) * this.quantity;
    }

    public double getUnrealizedPnLPercent(double currentPrice) {
        if (this.avgPrice == 0) return 0;
        return ((currentPrice - this.avgPrice) / this.avgPrice) * 100;
    }
}
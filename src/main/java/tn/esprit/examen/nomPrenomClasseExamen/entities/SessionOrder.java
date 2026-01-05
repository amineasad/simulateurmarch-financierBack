// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/entities/SessionOrder.java
package tn.esprit.examen.nomPrenomClasseExamen.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_orders", indexes = {
        @Index(name = "idx_session_symbol", columnList = "session_id, symbol"),
        @Index(name = "idx_session_user", columnList = "session_id, user_id"),
        @Index(name = "idx_order_time", columnList = "order_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Relations =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    private TradingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    // ===== Ordre Details =====

    @Column(nullable = false, length = 16)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer filledQuantity = 0;

    @Column(nullable = false)
    private Integer remainingQuantity;

    // ✅ CORRECTION: Retrait de precision/scale pour compatibilité MySQL
    @Column
    private Double price;

    @Column
    private Double executionPrice;

    // ===== Status =====

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    // ===== Timestamps =====

    @Column(nullable = false)
    private LocalDateTime orderTime;

    private LocalDateTime executionTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== Pour le carnet d'ordres =====

    @Transient
    private Long sequence; // Numéro de séquence pour le matching (pas persisté)

    // ===== Champs transients pour JSON deserialization =====

    @Transient
    private Long sessionId; // Pour recevoir du front

    @Transient
    private Long userId; // Pour recevoir du front

    // ===== Lifecycle Callbacks =====

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.orderTime == null) {
            this.orderTime = LocalDateTime.now();
        }
        if (this.filledQuantity == null) {
            this.filledQuantity = 0;
        }
        if (this.remainingQuantity == null) {
            this.remainingQuantity = this.quantity;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PostLoad
    protected void onLoad() {
        // Synchroniser les transients après chargement
        if (this.session != null) {
            this.sessionId = this.session.getId();
        }
        if (this.user != null) {
            this.userId = this.user.getId();
        }
    }

    // ===== Business Methods =====

    public boolean isExecutable() {
        if (this.status != OrderStatus.PENDING) {
            return false;
        }

        if (this.remainingQuantity == null || this.remainingQuantity <= 0) {
            return false;
        }

        // Pour LIMIT, le prix doit être > 0
        if (this.type == OrderType.LIMIT) {
            return this.price != null && this.price > 0;
        }

        // Pour MARKET, OK même si price est null ou 0
        return true;
    }

    public boolean isActive() {
        // Un ordre est actif s'il est PENDING et a encore de la quantité restante
        return this.status == OrderStatus.PENDING && this.remainingQuantity > 0;
    }

    public boolean isTerminal() {
        return this.status == OrderStatus.EXECUTED
                || this.status == OrderStatus.CANCELLED
                || this.status == OrderStatus.REJECTED;
    }

    public boolean isPartiallyFilled() {
        return this.filledQuantity > 0 && this.remainingQuantity > 0;
    }

    public double getFilledPercentage() {
        if (quantity == null || quantity == 0) return 0;
        return (double) filledQuantity / quantity * 100;
    }
}
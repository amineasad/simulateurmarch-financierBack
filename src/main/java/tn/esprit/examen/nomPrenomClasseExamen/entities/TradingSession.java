// tn.esprit.examen.nomPrenomClasseExamen.entities.TradingSession

package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trading_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TradingSession implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false)
    private LocalDateTime heureDebut;  // ✅ Changé de Instant à LocalDateTime

    @Column(nullable = false)
    private LocalDateTime heureFin;

    @Column(nullable = false)
    private Integer dureeMinutes;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Boolean modeAccelere;

    @Column(nullable = false)
    private Double cashInitial;

    @Column(unique = true, length = 20)
    private String codeAcces;

    @Column(nullable = false)
    private Long createurId;

    // Relations
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<SessionParticipation> participations = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<MarketEvent> evenements = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<SessionOrder> ordres = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
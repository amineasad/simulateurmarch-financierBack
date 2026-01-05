// tn.esprit.examen.nomPrenomClasseExamen.entities.MarketEvent

package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketEvent implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    private TradingSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSeverity severite;

    @Column
    private LocalDateTime declenchementPrevu;

    @Column
    private LocalDateTime declenchementReel;

    @Column(nullable = false)
    private Boolean declenche;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String impactsJson;

    @Column
    private Double impactGlobal;

    @Column
    private Integer dureeMinutes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (declenche == null) declenche = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
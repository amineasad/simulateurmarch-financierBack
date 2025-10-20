// tn.esprit.examen.nomPrenomClasseExamen.entities.SessionParticipation

package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_participations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    private TradingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    private Double cashActuel;

    @Column(nullable = false)
    private Double valeurPortefeuille;

    @Column(nullable = false)
    private Double rendement;

    @Column
    private Integer classement;

    @Column(nullable = false)
    private Boolean connecte;

    @Column
    private LocalDateTime heureConnexion;

    @Column
    private LocalDateTime heureDeconnexion;

    @Column(nullable = false)
    private Integer nombreOrdres;

    @Column(nullable = false)
    private Double volumeTotal;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (nombreOrdres == null) nombreOrdres = 0;
        if (volumeTotal == null) volumeTotal = 0.0;
        if (connecte == null) connecte = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
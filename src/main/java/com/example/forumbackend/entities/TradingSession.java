package com.example.forumbackend.entities;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trading_sessions")
@Getter @Setter
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

    // ========== TIMING RÉEL ==========
    @Column(nullable = false)
    private LocalDateTime heureDebut;  // Début réel (ex: 14:00)

    @Column(nullable = false)
    private LocalDateTime heureFin;    // Fin réelle (ex: 15:00)

    @Column(nullable = false)
    private Integer dureeMinutes;      // ⚠️ DEPRECATED - Utilisé pour compatibilité

    // ✅ NOUVEAUX CHAMPS TIME SCALING
    /**
     * Durée RÉELLE de la session en minutes
     * Ex: 60 = 1 heure réelle de jeu
     */
    @Column(name = "duration_minutes_real")
    private Integer durationMinutesReal = 60;

    /**
     * Facteur d'accélération temporelle
     * Ex: 6.5 = 1 minute réelle = 6.5 minutes virtuelles
     * Donc 60 min réelles = 390 min virtuelles (6h30 de marché)
     */
    @Column(name = "time_scale_factor")
    private Double timeScaleFactor = 6.5;

    // ========== CONFIGURATION ==========
    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Boolean modeAccelere;  // ⚠️ Peut être supprimé (redondant avec timeScaleFactor)

    @Column(nullable = false)
    private Double cashInitial;

    @Column(unique = true, length = 20)
    private String codeAcces;

    @Column(nullable = false)
    private Long createurId;

    // ========== RELATIONS ==========
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private Set<SessionParticipation> participations = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private Set<MarketEvent> evenements = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private Set<SessionOrder> ordres = new HashSet<>();

    // ========== TIMESTAMPS ==========
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // ✅ Initialiser durationMinutesReal si non défini
        if (durationMinutesReal == null) {
            durationMinutesReal = dureeMinutes != null ? dureeMinutes : 60;
        }

        // ✅ Initialiser timeScaleFactor si non défini
        if (timeScaleFactor == null) {
            timeScaleFactor = modeAccelere != null && modeAccelere ? 6.5 : 1.0;
        }

        // ✅ Maintenir la compatibilité avec dureeMinutes
        if (dureeMinutes == null) {
            dureeMinutes = durationMinutesReal;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== MÉTHODES TIME SCALING ==========

    /**
     * ✅ Retourne l'heure virtuelle du marché à un instant donné
     *
     * PRINCIPE:
     * - Marché virtuel : 09:30 → 16:00 (390 minutes)
     * - Temps réel : 60 minutes
     * - Facteur : 6.5x
     *
     * EXEMPLE:
     * - Session démarre à 14:00 réel → Marché ouvre à 09:30 virtuel
     * - À 14:10 réel (10 min) → 09:30 + (10 × 6.5) = 09:30 + 65min = 10:35 virtuel
     * - À 15:00 réel (60 min) → 09:30 + (60 × 6.5) = 09:30 + 390min = 16:00 virtuel
     */
    public LocalTime getVirtualMarketTime(LocalDateTime realTime) {
        if (heureDebut == null || realTime.isBefore(heureDebut)) {
            return LocalTime.of(9, 30); // Marché pas encore ouvert
        }

        // Temps réel écoulé en minutes
        long realMinutesElapsed = Duration.between(heureDebut, realTime).toMinutes();

        // Appliquer le facteur d'accélération
        double factor = (timeScaleFactor != null && timeScaleFactor > 0) ? timeScaleFactor : 1.0;
        long virtualMinutesElapsed = (long) (realMinutesElapsed * factor);

        // Ajouter au temps d'ouverture du marché (09:30)
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime virtualTime = marketOpen.plusMinutes(virtualMinutesElapsed);

        // Limiter à la fermeture du marché (16:00)
        LocalTime marketClose = LocalTime.of(16, 0);
        if (virtualTime.isAfter(marketClose)) {
            return marketClose;
        }

        return virtualTime;
    }

    /**
     * ✅ Retourne l'heure virtuelle actuelle du marché
     */
    public LocalTime getCurrentVirtualMarketTime() {
        return getVirtualMarketTime(LocalDateTime.now());
    }

    /**
     * ✅ Calcule le pourcentage de progression de la session (temps RÉEL)
     * Retourne une valeur entre 0.0 et 100.0
     */
    public double getProgressPercentage() {
        if (heureDebut == null || heureFin == null) {
            return 0.0;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(heureDebut)) {
            return 0.0;
        }

        if (now.isAfter(heureFin)) {
            return 100.0;
        }

        long totalMinutes = Duration.between(heureDebut, heureFin).toMinutes();
        long elapsedMinutes = Duration.between(heureDebut, now).toMinutes();

        if (totalMinutes == 0) {
            return 100.0;
        }

        return Math.min(100.0, (elapsedMinutes * 100.0) / totalMinutes);
    }

    /**
     * ✅ Vérifie si le marché virtuel est ouvert (09:30 - 16:00)
     */
    public boolean isVirtualMarketOpen() {
        LocalTime virtualTime = getCurrentVirtualMarketTime();
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime marketClose = LocalTime.of(16, 0);

        return !virtualTime.isBefore(marketOpen) && !virtualTime.isAfter(marketClose);
    }

    /**
     * ✅ Retourne le temps réel restant en minutes
     */
    public long getRealMinutesRemaining() {
        if (heureFin == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(heureFin)) {
            return 0;
        }

        return Duration.between(now, heureFin).toMinutes();
    }

    /**
     * ✅ Retourne le temps virtuel restant en minutes
     */
    public long getVirtualMinutesRemaining() {
        LocalTime virtualNow = getCurrentVirtualMarketTime();
        LocalTime marketClose = LocalTime.of(16, 0);

        if (virtualNow.isAfter(marketClose)) {
            return 0;
        }

        return Duration.between(virtualNow, marketClose).toMinutes();
    }

    /**
     * ✅ Détermine la phase de trading actuelle
     */
    public TradingPhase getCurrentTradingPhase() {
        LocalTime virtualTime = getCurrentVirtualMarketTime();

        if (virtualTime.isBefore(LocalTime.of(9, 30))) {
            return TradingPhase.PRE_MARKET;
        } else if (virtualTime.isBefore(LocalTime.of(10, 30))) {
            return TradingPhase.OPENING;  // Première heure (volatilité haute)
        } else if (virtualTime.isBefore(LocalTime.of(15, 0))) {
            return TradingPhase.MID_DAY;  // Milieu de journée
        } else if (virtualTime.isBefore(LocalTime.of(16, 0))) {
            return TradingPhase.CLOSING;  // Dernière heure (volatilité haute)
        } else {
            return TradingPhase.AFTER_MARKET;
        }
    }

    /**
     * ✅ Retourne une description lisible de la configuration time scaling
     */
    public String getTimeScaleDescription() {
        if (timeScaleFactor == null || durationMinutesReal == null) {
            return "Configuration invalide";
        }

        long virtualMinutes = (long) (durationMinutesReal * timeScaleFactor);
        long virtualHours = virtualMinutes / 60;
        long virtualMins = virtualMinutes % 60;

        return String.format(
                "%d min réelles = %dh%02d virtuelles (×%.1f)",
                durationMinutesReal,
                virtualHours,
                virtualMins,
                timeScaleFactor
        );
    }

    /**
     * ✅ Vérifie si la session est terminée (temps réel)
     */
    public boolean isRealTimeExpired() {
        if (heureFin == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(heureFin);
    }

    /**
     * ✅ Vérifie si le marché virtuel est fermé
     */
    public boolean isVirtualMarketClosed() {
        LocalTime virtualTime = getCurrentVirtualMarketTime();
        return virtualTime.isAfter(LocalTime.of(16, 0)) || virtualTime.equals(LocalTime.of(16, 0));
    }

    // ========== HELPERS POUR COMPATIBILITÉ ==========

    /**
     * ⚠️ DEPRECATED - Utiliser durationMinutesReal à la place
     */
    @Deprecated
    public Integer getDureeMinutes() {
        return durationMinutesReal != null ? durationMinutesReal : dureeMinutes;
    }

    /**
     * ⚠️ DEPRECATED - Utiliser setDurationMinutesReal à la place
     */
    @Deprecated
    public void setDureeMinutes(Integer dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
        if (this.durationMinutesReal == null) {
            this.durationMinutesReal = dureeMinutes;
        }
    }

    // ========== ENUM ==========

    /**
     * Phases de la journée de trading virtuelle
     */
    public enum TradingPhase {
        PRE_MARKET("Pré-ouverture", "⏰"),
        OPENING("Ouverture", "🔴"),           // 09:30 - 10:30 (volatilité haute)
        MID_DAY("Milieu de journée", "🟢"),   // 10:30 - 15:00 (calme)
        CLOSING("Clôture", "🟠"),             // 15:00 - 16:00 (volatilité haute)
        AFTER_MARKET("Après-clôture", "⚫");

        private final String description;
        private final String emoji;

        TradingPhase(String description, String emoji) {
            this.description = description;
            this.emoji = emoji;
        }

        public String getDescription() {
            return description;
        }

        public String getEmoji() {
            return emoji;
        }

        public String getFullDescription() {
            return emoji + " " + description;
        }
    }
}

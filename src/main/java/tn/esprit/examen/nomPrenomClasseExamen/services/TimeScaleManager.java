// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/services/TimeScaleManager.java
package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;

/**
 * Gère la compression temporelle pour les sessions de trading
 *
 * PRINCIPE :
 * - 1 heure réelle = 1 journée de marché (9h30 → 16h00 = 6h30 de trading)
 * - Facteur d'accélération : 6.5x (390 minutes simulées en 60 minutes réelles)
 *
 * EXEMPLE :
 * Session démarre à 14:00 réel
 * → Marché virtuel commence à 09:30
 * → À 14:15 réel, on est à 11:07 virtuel (1h37 de marché simulé)
 */
@Service
@Slf4j
public class TimeScaleManager {

    // ========== CONFIGURATION ==========

    // Heures de marché virtuelles
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    // Durée totale du marché virtuel en minutes
    private static final long VIRTUAL_MARKET_MINUTES = ChronoUnit.MINUTES.between(MARKET_OPEN, MARKET_CLOSE); // 390 min

    // Durée réelle de la session (configurable)
    private static final long REAL_SESSION_MINUTES = 60; // 1 heure

    // Facteur d'accélération
    private static final double TIME_SCALE_FACTOR = (double) VIRTUAL_MARKET_MINUTES / REAL_SESSION_MINUTES; // 6.5x

    // ========== CONVERSIONS ==========

    /**
     * Convertit le temps réel écoulé en temps virtuel de marché
     *
     * @param sessionStartReal Instant de début de session (temps réel)
     * @param currentReal Instant actuel (temps réel)
     * @return L'heure virtuelle du marché
     */
    public LocalTime getVirtualMarketTime(Instant sessionStartReal, Instant currentReal) {
        // Temps réel écoulé en minutes
        long realMinutesElapsed = ChronoUnit.MINUTES.between(sessionStartReal, currentReal);

        // Appliquer le facteur d'accélération
        long virtualMinutesElapsed = (long) (realMinutesElapsed * TIME_SCALE_FACTOR);

        // Ajouter au temps d'ouverture du marché
        LocalTime virtualTime = MARKET_OPEN.plusMinutes(virtualMinutesElapsed);

        // Limiter à la fermeture du marché
        if (virtualTime.isAfter(MARKET_CLOSE)) {
            return MARKET_CLOSE;
        }

        return virtualTime;
    }

    /**
     * Calcule le pourcentage de progression de la journée de trading
     *
     * @param virtualTime Heure virtuelle actuelle
     * @return Pourcentage entre 0.0 et 1.0
     */
    public double getMarketProgressPercentage(LocalTime virtualTime) {
        if (virtualTime.isBefore(MARKET_OPEN)) return 0.0;
        if (virtualTime.isAfter(MARKET_CLOSE)) return 1.0;

        long minutesFromOpen = ChronoUnit.MINUTES.between(MARKET_OPEN, virtualTime);
        return (double) minutesFromOpen / VIRTUAL_MARKET_MINUTES;
    }

    /**
     * Vérifie si le marché est ouvert à l'heure virtuelle donnée
     */
    public boolean isMarketOpen(LocalTime virtualTime) {
        return !virtualTime.isBefore(MARKET_OPEN) && !virtualTime.isAfter(MARKET_CLOSE);
    }

    /**
     * Calcule combien de temps réel reste avant la fermeture
     *
     * @param sessionStartReal Début de session
     * @param currentReal Instant actuel
     * @return Minutes réelles restantes (peut être négatif si fermé)
     */
    public long getRealMinutesUntilClose(Instant sessionStartReal, Instant currentReal) {
        LocalTime virtualNow = getVirtualMarketTime(sessionStartReal, currentReal);

        if (virtualNow.isAfter(MARKET_CLOSE)) {
            return 0;
        }

        long virtualMinutesLeft = ChronoUnit.MINUTES.between(virtualNow, MARKET_CLOSE);
        return (long) (virtualMinutesLeft / TIME_SCALE_FACTOR);
    }

    /**
     * Détermine dans quelle phase de trading on se trouve
     */
    public TradingPhase getCurrentPhase(LocalTime virtualTime) {
        if (virtualTime.isBefore(MARKET_OPEN)) {
            return TradingPhase.PRE_MARKET;
        } else if (virtualTime.isBefore(LocalTime.of(10, 30))) {
            return TradingPhase.OPENING;
        } else if (virtualTime.isBefore(LocalTime.of(15, 0))) {
            return TradingPhase.MID_DAY;
        } else if (virtualTime.isBefore(MARKET_CLOSE)) {
            return TradingPhase.CLOSING;
        } else {
            return TradingPhase.AFTER_MARKET;
        }
    }

    /**
     * Génère des statistiques pour le monitoring
     */
    public TimeScaleStats getStats(Instant sessionStartReal, Instant currentReal) {
        LocalTime virtualTime = getVirtualMarketTime(sessionStartReal, currentReal);
        double progress = getMarketProgressPercentage(virtualTime);
        long realMinutesElapsed = ChronoUnit.MINUTES.between(sessionStartReal, currentReal);
        long realMinutesLeft = getRealMinutesUntilClose(sessionStartReal, currentReal);

        return TimeScaleStats.builder()
                .virtualMarketTime(virtualTime)
                .tradingPhase(getCurrentPhase(virtualTime))
                .progressPercentage(progress)
                .realMinutesElapsed(realMinutesElapsed)
                .realMinutesRemaining(realMinutesLeft)
                .timeScaleFactor(TIME_SCALE_FACTOR)
                .isMarketOpen(isMarketOpen(virtualTime))
                .build();
    }

    // ========== DTOs ==========

    public enum TradingPhase {
        PRE_MARKET("Pré-ouverture"),
        OPENING("Ouverture (volatilité haute)"),
        MID_DAY("Milieu de journée"),
        CLOSING("Clôture (volatilité haute)"),
        AFTER_MARKET("Après-clôture");

        private final String description;

        TradingPhase(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class TimeScaleStats {
        private LocalTime virtualMarketTime;
        private TradingPhase tradingPhase;
        private double progressPercentage;
        private long realMinutesElapsed;
        private long realMinutesRemaining;
        private double timeScaleFactor;
        private boolean isMarketOpen;

        public String getFormattedProgress() {
            return String.format("%.1f%%", progressPercentage * 100);
        }

        public String getVirtualTimeFormatted() {
            return virtualMarketTime.toString();
        }
    }
}
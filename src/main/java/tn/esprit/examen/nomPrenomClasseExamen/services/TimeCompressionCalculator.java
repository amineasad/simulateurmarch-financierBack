package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Calculateur de compression temporelle pour simulateur boursier éducatif
 *
 * OBJECTIF : 252 jours de trading (1 année) en 60 minutes de jeu réel
 *
 * CALCULS :
 * - 252 jours ouvrés × 6.5 heures par jour = 1,638 heures de marché
 * - 1,638 heures × 60 minutes = 98,280 minutes virtuelles
 * - 98,280 minutes / 60 minutes réelles = 1,638x d'accélération
 *
 * Mais si on part de données 1-minute :
 * - 98,280 candles 1-min à lire en 60 minutes réelles
 * - 98,280 / 60 = 1,638 candles par minute réelle
 * - Soit 1 candle toutes les 36.6 millisecondes
 */
@Component
@Slf4j
public class TimeCompressionCalculator {

    // ========== CONSTANTES ==========

    /** Nombre de jours de trading par an (hors week-ends et jours fériés) */
    public static final int TRADING_DAYS_PER_YEAR = 252;

    /** Heures de marché par jour (9:30 AM - 4:00 PM = 6.5 heures) */
    public static final double MARKET_HOURS_PER_DAY = 6.5;

    /** Minutes de marché par jour */
    public static final int MARKET_MINUTES_PER_DAY = (int)(MARKET_HOURS_PER_DAY * 60); // 390 min

    /** Total de minutes de marché dans une année */
    public static final int TOTAL_MARKET_MINUTES = TRADING_DAYS_PER_YEAR * MARKET_MINUTES_PER_DAY; // 98,280 min

    /** Durée cible du jeu en minutes réelles */
    public static final int TARGET_GAME_DURATION_MINUTES = 60;

    /** Facteur d'accélération exact */
    public static final double EXACT_ACCELERATION_FACTOR =
            (double) TOTAL_MARKET_MINUTES / TARGET_GAME_DURATION_MINUTES; // 1,638x

    /** Millisecondes réelles entre chaque tick 1-minute */
    public static final double MS_PER_VIRTUAL_MINUTE =
            (TARGET_GAME_DURATION_MINUTES * 60.0 * 1000.0) / TOTAL_MARKET_MINUTES; // ~36.6 ms

    // ========== CONFIGURATION DYNAMIQUE ==========

    /**
     * Configuration de compression temporelle
     */
    @Data
    public static class CompressionConfig {
        /** Durée cible du jeu en minutes réelles */
        private int targetDurationMinutes;

        /** Facteur d'accélération calculé */
        private double accelerationFactor;

        /** Millisecondes réelles entre chaque tick virtuel */
        private double msPerTick;

        /** Nombre total de ticks à diffuser */
        private int totalTicks;

        /** Date de début virtuelle */
        private LocalDateTime virtualStartDate;

        /** Date de fin virtuelle */
        private LocalDateTime virtualEndDate;

        /** Vitesse actuelle (multiplicateur du facteur de base) */
        private double speedMultiplier = 1.0;

        /** Est en pause */
        private boolean paused = false;
    }

    /**
     * Calcule la configuration pour une compression temporelle donnée
     *
     * @param virtualStartDate Date de début virtuelle (ex: 2023-01-01 09:30)
     * @param virtualEndDate Date de fin virtuelle (ex: 2023-12-31 16:00)
     * @param targetDurationMinutes Durée cible en minutes réelles (ex: 60)
     * @return Configuration complète
     */
    public CompressionConfig calculateConfig(
            LocalDateTime virtualStartDate,
            LocalDateTime virtualEndDate,
            int targetDurationMinutes) {

        CompressionConfig config = new CompressionConfig();
        config.setVirtualStartDate(virtualStartDate);
        config.setVirtualEndDate(virtualEndDate);
        config.setTargetDurationMinutes(targetDurationMinutes);

        // Calculer le nombre total de minutes de marché dans la période
        int totalVirtualMinutes = countTradingMinutes(virtualStartDate, virtualEndDate);
        config.setTotalTicks(totalVirtualMinutes);

        // Calculer le facteur d'accélération
        double accelerationFactor = (double) totalVirtualMinutes / targetDurationMinutes;
        config.setAccelerationFactor(accelerationFactor);

        // Calculer les millisecondes réelles entre chaque tick
        double msPerTick = (targetDurationMinutes * 60.0 * 1000.0) / totalVirtualMinutes;
        config.setMsPerTick(msPerTick);

        log.info("📊 Configuration de compression temporelle:");
        log.info("   Virtual period: {} → {}", virtualStartDate, virtualEndDate);
        log.info("   Total virtual minutes: {}", totalVirtualMinutes);
        log.info("   Target duration: {} min ({} hours)", targetDurationMinutes, targetDurationMinutes / 60.0);
        log.info("   Acceleration factor: {:.2f}x", accelerationFactor);
        log.info("   MS per tick: {:.2f} ms", msPerTick);
        log.info("   Total ticks: {}", totalVirtualMinutes);

        return config;
    }

    /**
     * Compte le nombre de minutes de marché entre deux dates
     * (en excluant les week-ends et les heures hors marché)
     *
     * Version simplifiée : suppose tous les jours ouvrés
     * Version complète devrait vérifier un calendrier de jours fériés
     */
    private int countTradingMinutes(LocalDateTime start, LocalDateTime end) {
        int totalMinutes = 0;
        LocalDateTime current = start;

        while (current.isBefore(end)) {
            // Vérifier si c'est un jour ouvré (lundi-vendredi)
            if (isWeekday(current)) {
                // Vérifier si c'est pendant les heures de marché (9:30 - 16:00)
                if (isDuringMarketHours(current.toLocalTime())) {
                    totalMinutes++;
                }
            }

            // Avancer d'une minute
            current = current.plusMinutes(1);
        }

        return totalMinutes;
    }

    /**
     * Vérifie si c'est un jour de semaine
     */
    private boolean isWeekday(LocalDateTime dateTime) {
        int dayOfWeek = dateTime.getDayOfWeek().getValue();
        return dayOfWeek >= 1 && dayOfWeek <= 5; // Lundi = 1, Vendredi = 5
    }

    /**
     * Vérifie si l'heure est pendant les heures de marché (9:30 - 16:00)
     */
    private boolean isDuringMarketHours(LocalTime time) {
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime marketClose = LocalTime.of(16, 0);
        return !time.isBefore(marketOpen) && time.isBefore(marketClose);
    }

    /**
     * Calcule le temps virtuel écoulé depuis le début
     *
     * @param config Configuration de compression
     * @param realTimeElapsedMs Temps réel écoulé en millisecondes
     * @return Temps virtuel correspondant
     */
    public LocalDateTime calculateVirtualTime(CompressionConfig config, long realTimeElapsedMs) {
        // Appliquer le multiplicateur de vitesse
        double effectiveMsPerTick = config.getMsPerTick() / config.getSpeedMultiplier();

        // Calculer le nombre de ticks écoulés
        int ticksElapsed = (int) (realTimeElapsedMs / effectiveMsPerTick);

        // Ajouter les ticks à la date de début
        LocalDateTime virtualTime = config.getVirtualStartDate().plusMinutes(ticksElapsed);

        // Limiter à la date de fin
        if (virtualTime.isAfter(config.getVirtualEndDate())) {
            return config.getVirtualEndDate();
        }

        return virtualTime;
    }

    /**
     * Calcule l'index du tick actuel
     */
    public int calculateCurrentTick(CompressionConfig config, long realTimeElapsedMs) {
        double effectiveMsPerTick = config.getMsPerTick() / config.getSpeedMultiplier();
        int tick = (int) (realTimeElapsedMs / effectiveMsPerTick);
        return Math.min(tick, config.getTotalTicks() - 1);
    }

    /**
     * Calcule le pourcentage de progression
     */
    public double calculateProgress(CompressionConfig config, long realTimeElapsedMs) {
        int currentTick = calculateCurrentTick(config, realTimeElapsedMs);
        return (double) currentTick / config.getTotalTicks() * 100.0;
    }

    /**
     * Crée une configuration pour les cas d'usage courants
     */
    public static class PresetConfigs {

        /** 1 année complète (252 jours) en 60 minutes */
        public static CompressionConfig fullYear60Minutes() {
            TimeCompressionCalculator calc = new TimeCompressionCalculator();
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 9, 30);
            LocalDateTime end = LocalDateTime.of(2023, 12, 31, 16, 0);
            return calc.calculateConfig(start, end, 60);
        }

        /** 1 mois (21 jours) en 30 minutes */
        public static CompressionConfig oneMonth30Minutes() {
            TimeCompressionCalculator calc = new TimeCompressionCalculator();
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 9, 30);
            LocalDateTime end = LocalDateTime.of(2023, 1, 31, 16, 0);
            return calc.calculateConfig(start, end, 30);
        }

        /** 1 semaine (5 jours) en 15 minutes */
        public static CompressionConfig oneWeek15Minutes() {
            TimeCompressionCalculator calc = new TimeCompressionCalculator();
            LocalDateTime start = LocalDateTime.of(2023, 1, 2, 9, 30); // Lundi
            LocalDateTime end = LocalDateTime.of(2023, 1, 6, 16, 0);   // Vendredi
            return calc.calculateConfig(start, end, 15);
        }

        /** 1 jour en 2 minutes (pour tests rapides) */
        public static CompressionConfig oneDay2Minutes() {
            TimeCompressionCalculator calc = new TimeCompressionCalculator();
            LocalDateTime start = LocalDateTime.of(2023, 1, 2, 9, 30);
            LocalDateTime end = LocalDateTime.of(2023, 1, 2, 16, 0);
            return calc.calculateConfig(start, end, 2);
        }
    }
}
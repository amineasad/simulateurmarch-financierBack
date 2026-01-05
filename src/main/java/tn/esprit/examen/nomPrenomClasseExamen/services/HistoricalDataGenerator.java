package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.services.MarketReplayEngine.MarketTick;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ✨ Génère des données historiques pour TOUS les symboles simultanément
 */
@Service
@Slf4j
public class HistoricalDataGenerator {

    private final Random random = new Random();

    /**
     * ✨ Génère une année complète pour TOUS les symboles
     *
     * IMPORTANT: Génère les ticks pour tous les symboles au MÊME timestamp
     *
     * @param symbols Liste de TOUS les symboles (actions + forex + métaux + etf)
     * @param startDate Date de début
     * @return Liste de ticks GROUPÉS par timestamp
     */
    public List<MarketTick> generateYearData(List<String> symbols, LocalDateTime startDate) {
        List<MarketTick> allTicks = new ArrayList<>();

        log.info("🔄 Génération de données historiques...");
        log.info("   Symboles: {}", symbols);
        log.info("   Date début: {}", startDate);

        LocalDateTime current = startDate;
        LocalDateTime endDate = startDate.plusYears(1);

        // ✨ Initialiser les prix de départ pour chaque symbole
        java.util.Map<String, Double> currentPrices = new java.util.HashMap<>();
        for (String symbol : symbols) {
            currentPrices.put(symbol, getInitialPrice(symbol));
        }

        int totalTicks = 0;

        while (current.isBefore(endDate)) {
            // Vérifier si c'est un jour ouvré
            if (isWeekday(current)) {
                // ✨ Générer les ticks de la journée pour TOUS les symboles
                List<MarketTick> dayTicks = generateDayDataAllSymbols(
                        symbols,
                        current,
                        currentPrices
                );

                allTicks.addAll(dayTicks);
                totalTicks += dayTicks.size();

                // Mettre à jour les prix de clôture
                for (MarketTick tick : dayTicks) {
                    if (tick.getTimestamp().toLocalTime().equals(LocalTime.of(15, 59))) {
                        // Dernier tick de la journée
                        currentPrices.put(tick.getSymbol(), tick.getClose());
                    }
                }
            }

            // Passer au jour suivant
            current = current.plusDays(1);
        }

        log.info("✅ Total: {} ticks générés pour {} symboles", totalTicks, symbols.size());
        log.info("   Soit {} ticks par symbole en moyenne", totalTicks / symbols.size());

        return allTicks;
    }

    /**
     * ✨ Génère les ticks d'une journée pour TOUS les symboles SIMULTANÉMENT
     *
     * Résultat: À chaque minute (09:30, 09:31, ...), on a un tick pour CHAQUE symbole
     */
    private List<MarketTick> generateDayDataAllSymbols(
            List<String> symbols,
            LocalDateTime date,
            java.util.Map<String, Double> currentPrices) {

        List<MarketTick> dayTicks = new ArrayList<>();

        LocalDateTime currentTime = date
                .withHour(9)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);

        LocalDateTime marketClose = date
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        while (currentTime.isBefore(marketClose)) {
            // ✨ Pour CHAQUE minute, générer un tick pour TOUS les symboles
            for (String symbol : symbols) {
                double baseVolatility = getBaseVolatility(symbol);
                double volatilityMultiplier = getIntraDayVolatilityMultiplier(currentTime.toLocalTime());
                double effectiveVolatility = baseVolatility * volatilityMultiplier;

                double currentPrice = currentPrices.get(symbol);

                MarketTick tick = generateMinuteTick(
                        symbol,
                        currentTime,
                        currentPrice,
                        effectiveVolatility
                );

                dayTicks.add(tick);

                // Mettre à jour le prix actuel
                currentPrices.put(symbol, tick.getClose());
            }

            currentTime = currentTime.plusMinutes(1);
        }

        return dayTicks;
    }

    /**
     * Génère un tick OHLC pour une minute
     */
    private MarketTick generateMinuteTick(
            String symbol,
            LocalDateTime timestamp,
            double openPrice,
            double volatility) {

        MarketTick tick = new MarketTick();
        tick.setTimestamp(timestamp);
        tick.setSymbol(symbol);
        tick.setOpen(openPrice);

        // Générer high, low, close avec un mouvement brownien
        double range = openPrice * volatility / 100.0;

        double high = openPrice + (random.nextDouble() * range);
        double low = openPrice - (random.nextDouble() * range);

        if (high < low) {
            double temp = high;
            high = low;
            low = temp;
        }

        if (openPrice > high) high = openPrice;
        if (openPrice < low) low = openPrice;

        double close = low + (random.nextDouble() * (high - low));

        tick.setHigh(Math.max(high, openPrice));
        tick.setLow(Math.min(low, openPrice));
        tick.setClose(close);

        // Volume aléatoire
        tick.setVolume((long) (100000 + random.nextInt(900000)));

        return tick;
    }

    /**
     * Prix initial selon le type d'actif
     */
    private double getInitialPrice(String symbol) {
        if (symbol.contains("/")) {
            // Forex
            if (symbol.startsWith("XAU")) return 1900.0; // Or
            if (symbol.startsWith("XAG")) return 23.0;   // Argent
            if (symbol.startsWith("XPT")) return 950.0;  // Platine
            if (symbol.startsWith("XPD")) return 1800.0; // Palladium
            if (symbol.startsWith("EUR")) return 1.08;
            if (symbol.startsWith("GBP")) return 1.27;
            if (symbol.contains("JPY")) return 145.0;
            return 1.0;
        } else {
            // Actions/ETF : entre 50 et 500
            // Quelques prix réalistes pour les actions connues
            switch (symbol) {
                case "AAPL": return 180.0;
                case "MSFT": return 380.0;
                case "GOOGL": return 140.0;
                case "AMZN": return 170.0;
                case "META": return 350.0;
                case "TSLA": return 240.0;
                case "NVDA": return 500.0;
                case "SPY": return 450.0;
                case "QQQ": return 380.0;
                default: return 100 + random.nextDouble() * 400;
            }
        }
    }

    /**
     * Volatilité de base selon le type d'actif
     */
    private double getBaseVolatility(String symbol) {
        if (symbol.contains("/")) {
            // Forex : faible volatilité (0.05-0.3%)
            if (symbol.startsWith("XAU") || symbol.startsWith("XAG")) {
                return 0.3 + random.nextDouble() * 0.5; // Métaux plus volatils
            }
            return 0.05 + random.nextDouble() * 0.25;
        } else if (symbol.equals("TSLA") || symbol.equals("NVDA")) {
            // Tech volatile : 1-3%
            return 1.0 + random.nextDouble() * 2.0;
        } else if (symbol.equals("SPY") || symbol.equals("QQQ") || symbol.equals("DIA")) {
            // ETF : faible volatilité
            return 0.3 + random.nextDouble() * 0.5;
        } else {
            // Actions normales : 0.5-1.5%
            return 0.5 + random.nextDouble() * 1.0;
        }
    }

    /**
     * Multiplicateur de volatilité intra-journée
     */
    private double getIntraDayVolatilityMultiplier(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();

        // 9:30-10:30 : Ouverture volatile (×2)
        if (hour == 9 || (hour == 10 && minute < 30)) {
            return 2.0;
        }
        // 15:00-16:00 : Clôture volatile (×1.8)
        else if (hour >= 15) {
            return 1.8;
        }
        // 10:30-15:00 : Calme (×1)
        else {
            return 1.0;
        }
    }

    /**
     * Vérifie si c'est un jour de semaine
     */
    private boolean isWeekday(LocalDateTime date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /**
     * ✨ Génère rapidement des données de test (1 semaine)
     * TOUS les symboles générés ensemble
     */
    public List<MarketTick> generateTestData(List<String> symbols) {
        log.info("🔄 Génération de données de TEST (1 semaine) pour {} symboles...", symbols.size());

        LocalDateTime startDate = LocalDateTime.of(2023, 1, 2, 9, 30); // Lundi
        LocalDateTime endDate = startDate.plusDays(5); // Vendredi

        List<MarketTick> allTicks = new ArrayList<>();

        // Initialiser les prix
        java.util.Map<String, Double> currentPrices = new java.util.HashMap<>();
        for (String symbol : symbols) {
            currentPrices.put(symbol, getInitialPrice(symbol));
        }

        LocalDateTime current = startDate;

        while (current.isBefore(endDate)) {
            if (isWeekday(current)) {
                List<MarketTick> dayTicks = generateDayDataAllSymbols(
                        symbols,
                        current,
                        currentPrices
                );
                allTicks.addAll(dayTicks);

                // Mettre à jour les prix de clôture
                for (MarketTick tick : dayTicks) {
                    if (tick.getTimestamp().toLocalTime().equals(LocalTime.of(15, 59))) {
                        currentPrices.put(tick.getSymbol(), tick.getClose());
                    }
                }
            }
            current = current.plusDays(1);
        }

        log.info("✅ {} ticks de test générés", allTicks.size());
        log.info("   {} ticks par symbole", allTicks.size() / symbols.size());

        return allTicks;
    }
}
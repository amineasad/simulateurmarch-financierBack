package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.dto.AllocationRequestDTO;
import tn.esprit.examen.nomPrenomClasseExamen.dto.AllocationResultDTO;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.PriceHistoryProvider;

import java.util.*;

/**
 * 💼 Service d'allocation de portefeuille.
 *
 * Deux stratégies :
 * - MAX_RETURN : privilégie les actifs avec meilleur rendement moyen
 * - LOW_VOL    : privilégie la faible volatilité
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioAllocationService {

    private final PriceHistoryProvider priceHistoryProvider;

    // Nombre de points d'historique utilisés (ex : 200 dernières minutes)
    private static final int HISTORY_POINTS = 200;

    public AllocationResultDTO allocate(AllocationRequestDTO req) {

        List<String> symbols = Optional.ofNullable(req.symbols())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("Liste de symboles vide"));

        double totalCapital = req.totalCapital();
        if (totalCapital <= 0) {
            throw new IllegalArgumentException("Le capital doit être > 0");
        }

        String strategy = req.strategy() == null ? "MAX_RETURN" : req.strategy().toUpperCase();

        // 1) Récupérer les historiques de prix
        Map<String, List<Double>> priceSeries =
                priceHistoryProvider.getClosePrices(req.sessionId(), symbols, HISTORY_POINTS);

        // 2) Calculer rendements et volatilité
        Map<String, Double> expectedReturns = new HashMap<>();
        Map<String, Double> volatilities = new HashMap<>();

        for (String symbol : symbols) {
            List<Double> prices = priceSeries.getOrDefault(symbol, List.of());

            if (prices.size() < 2) {
                // Pas assez de données : on met neutre
                expectedReturns.put(symbol, 0.0);
                volatilities.put(symbol, 0.0);
                continue;
            }

            List<Double> returns = new ArrayList<>();
            for (int i = 1; i < prices.size(); i++) {
                double prev = prices.get(i - 1);
                double curr = prices.get(i);
                if (prev > 0) {
                    double r = (curr - prev) / prev; // rendement simple
                    returns.add(r);
                }
            }

            if (returns.isEmpty()) {
                expectedReturns.put(symbol, 0.0);
                volatilities.put(symbol, 0.0);
                continue;
            }

            double mean = returns.stream().mapToDouble(d -> d).average().orElse(0.0);
            double variance = returns.stream()
                    .mapToDouble(r -> (r - mean) * (r - mean))
                    .average()
                    .orElse(0.0);

            double vol = Math.sqrt(variance);

            expectedReturns.put(symbol, mean);
            volatilities.put(symbol, vol);
        }

        // 3) Calculer les poids selon la stratégie
        Map<String, Double> weights;
        String comment;

        switch (strategy) {
            case "LOW_VOL":
                weights = computeLowVolWeights(symbols, volatilities);
                comment = "Allocation orientée risque modéré : plus de poids sur les actifs les moins volatils.";
                break;

            case "MAX_RETURN":
            default:
                weights = computeMaxReturnWeights(symbols, expectedReturns);
                comment = "Allocation orientée rendement : plus de poids sur les actifs avec meilleur rendement moyen.";
                break;
        }

        // 4) Calculer les montants
        Map<String, Double> amounts = new HashMap<>();
        for (String symbol : symbols) {
            double w = weights.getOrDefault(symbol, 0.0);
            amounts.put(symbol, totalCapital * w);
        }

        return new AllocationResultDTO(
                strategy,
                totalCapital,
                weights,
                amounts,
                expectedReturns,
                volatilities,
                comment
        );
    }

    /**
     * Stratégie MAX_RETURN :
     * - On garde uniquement les rendements positifs
     * - Poids proportionnels au rendement moyen
     * - Si tout est <= 0 → equal weight
     */
    private Map<String, Double> computeMaxReturnWeights(List<String> symbols,
                                                        Map<String, Double> expectedReturns) {
        Map<String, Double> weights = new HashMap<>();

        double sumPositive = expectedReturns.values().stream()
                .mapToDouble(r -> Math.max(0.0, r))
                .sum();

        if (sumPositive <= 0) {
            // Tout nul ou négatif → répartition égale
            double equal = 1.0 / symbols.size();
            for (String s : symbols) {
                weights.put(s, equal);
            }
            return weights;
        }

        for (String s : symbols) {
            double r = Math.max(0.0, expectedReturns.getOrDefault(s, 0.0));
            weights.put(s, r / sumPositive);
        }

        return weights;
    }

    /**
     * Stratégie LOW_VOL :
     * - Poids proportionnels à 1 / volatilité
     * - Si vol = 0 → on lui donne un poids neutre
     */
    private Map<String, Double> computeLowVolWeights(List<String> symbols,
                                                     Map<String, Double> volatilities) {
        Map<String, Double> invVol = new HashMap<>();

        for (String s : symbols) {
            double vol = volatilities.getOrDefault(s, 0.0);
            if (vol <= 0) {
                invVol.put(s, 1.0); // si on ne sait pas, on lui donne 1
            } else {
                invVol.put(s, 1.0 / vol);
            }
        }

        double sum = invVol.values().stream().mapToDouble(d -> d).sum();
        Map<String, Double> weights = new HashMap<>();

        if (sum <= 0) {
            double equal = 1.0 / symbols.size();
            for (String s : symbols) {
                weights.put(s, equal);
            }
            return weights;
        }

        for (String s : symbols) {
            weights.put(s, invVol.get(s) / sum);
        }

        return weights;
    }
}

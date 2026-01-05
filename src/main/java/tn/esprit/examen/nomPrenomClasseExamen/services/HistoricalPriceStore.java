package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🧠 Stocke en mémoire les derniers prix de clôture par
 * - session de replay
 * - symbole
 *
 * Objectif : fournir un historique de prix pour les algos d'allocation.
 */
@Service
@Slf4j
public class HistoricalPriceStore {

    // sessionId -> (symbol -> file de prix)
    private final Map<Long, Map<String, Deque<Double>>> store = new ConcurrentHashMap<>();

    // Nombre maximal de points gardés par symbole (par exemple 1000)
    private static final int MAX_POINTS_PER_SYMBOL = 1000;

    /**
     * Enregistre un nouveau prix de clôture
     */
    public void recordPrice(Long sessionId, String symbol, double close) {
        if (sessionId == null || symbol == null) return;

        store
                .computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>())
                .computeIfAbsent(symbol, s -> new ArrayDeque<>());

        Deque<Double> prices = store.get(sessionId).get(symbol);

        // Ajout en fin
        prices.addLast(close);

        // On coupe les très longues séries
        while (prices.size() > MAX_POINTS_PER_SYMBOL) {
            prices.removeFirst();
        }
    }

    /**
     * Récupère les N derniers prix de clôture pour chaque symbole
     */
    public Map<String, List<Double>> getLastClosePrices(Long sessionId,
                                                        List<String> symbols,
                                                        int n) {
        Map<String, List<Double>> result = new HashMap<>();

        if (!store.containsKey(sessionId) || symbols == null) {
            return result;
        }

        Map<String, Deque<Double>> sessionData = store.get(sessionId);

        for (String symbol : symbols) {
            Deque<Double> prices = sessionData.get(symbol);
            if (prices == null || prices.isEmpty()) {
                result.put(symbol, List.of());
                continue;
            }

            // On prend les n derniers
            List<Double> all = new ArrayList<>(prices);
            int fromIndex = Math.max(0, all.size() - n);
            List<Double> lastN = all.subList(fromIndex, all.size());

            result.put(symbol, new ArrayList<>(lastN));
        }

        return result;
    }

    /**
     * Supprime toutes les données pour une session (optionnel)
     */
    public void clearSession(Long sessionId) {
        store.remove(sessionId);
    }
}

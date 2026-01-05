package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import java.util.List;
import java.util.Map;

/**
 * Abstraction pour récupérer des séries de prix.
 * Pour l'instant : implémentation via le replay.
 * Plus tard : on pourra ajouter Alpha Vantage sans changer le reste.
 */
public interface PriceHistoryProvider {

    /**
     * Retourne les séries de prix de clôture pour une session donnée
     *
     * @param sessionId ID de la session de replay
     * @param symbols   Liste de symboles
     * @param n         Nombre de points (ex : 200 derniers)
     * @return Map<symbole, liste des prix>
     */
    Map<String, List<Double>> getClosePrices(Long sessionId, List<String> symbols, int n);
}

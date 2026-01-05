package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.PriceHistoryProvider;

import java.util.List;
import java.util.Map;

/**
 * Implémentation de PriceHistoryProvider basée sur
 * les données du MarketReplay (HistoricalPriceStore).
 */
@Service
@RequiredArgsConstructor
public class ReplayPriceHistoryProvider implements PriceHistoryProvider {

    private final HistoricalPriceStore priceStore;

    @Override
    public Map<String, List<Double>> getClosePrices(Long sessionId,
                                                    List<String> symbols,
                                                    int n) {
        return priceStore.getLastClosePrices(sessionId, symbols, n);
    }
}

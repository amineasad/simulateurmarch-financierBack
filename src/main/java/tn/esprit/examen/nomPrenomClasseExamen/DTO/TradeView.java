package tn.esprit.examen.nomPrenomClasseExamen.DTO;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Trade;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeView(
        Long id,
        Long assetId,
        BigDecimal price,
        Integer quantity,
        Instant executedAt
) {

    public static TradeView from(Trade trade) {
        return new TradeView(
                trade.getId(),
                trade.getAssetId(),
                trade.getPrice(),
                trade.getQuantity(),
                trade.getExecutedAt()
        );
    }
}

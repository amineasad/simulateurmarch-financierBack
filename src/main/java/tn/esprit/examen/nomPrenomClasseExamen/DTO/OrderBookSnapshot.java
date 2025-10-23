package tn.esprit.examen.nomPrenomClasseExamen.DTO;

import java.math.BigDecimal;
import java.util.List;

public record OrderBookSnapshot(
        List<OrderBookLevel> bids,
        List<OrderBookLevel> asks,
        BigDecimal bestBid,
        BigDecimal bestAsk
) {}

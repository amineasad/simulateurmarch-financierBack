package tn.esprit.examen.nomPrenomClasseExamen.DTO;

import java.math.BigDecimal;

public record OrderBookLevel(
        BigDecimal price,
        Integer quantity
) {}

package tn.esprit.examen.nomPrenomClasseExamen.DTO;

import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderType;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;

import java.math.BigDecimal;

public record CreateOrderDTO(
        Long Id,
        OrderSide OrderSide,
        OrderType type,
        BigDecimal price,
        Integer quantity
) {
    // Validation des données
    public boolean isValid() {
        return Id != null &&
                OrderSide != null &&
                type != null &&
                quantity != null &&
                quantity > 0 &&
                (type == OrderType.MARKET || (price != null && price.compareTo(BigDecimal.ZERO) > 0));
    }
}


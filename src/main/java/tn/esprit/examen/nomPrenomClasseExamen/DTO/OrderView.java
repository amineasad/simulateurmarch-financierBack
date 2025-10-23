package tn.esprit.examen.nomPrenomClasseExamen.DTO;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Order;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;

import java.math.BigDecimal;

public record OrderView(
        Long id,
        Long assetId,
        OrderSide OrderSide,
        OrderStatus status,
        Integer filled,
        Integer remaining,
        BigDecimal price,
        String message
) {

    public static OrderView from(Order order) {
        return new OrderView(
                order.getId(),
                order.getAsset().getId(),
                order.getSide(),
                order.getStatus(),
                order.getQuantity() - order.getRemainingQuantity(),
                order.getRemainingQuantity(),
                order.getPrice(),
                null
        );
    }

    public OrderView withError(String errorMessage) {
        return new OrderView(id, assetId, OrderSide, status, filled, remaining, price, errorMessage);
    }
}

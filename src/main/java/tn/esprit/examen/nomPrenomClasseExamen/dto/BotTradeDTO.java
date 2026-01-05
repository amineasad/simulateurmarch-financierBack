package tn.esprit.examen.nomPrenomClasseExamen.dto;

import lombok.Data;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;
import java.time.LocalDateTime;

@Data
public class BotTradeDTO {
    private Long id;
    private Long botId;
    private String symbol;
    private OrderSide side;
    private Double entryPrice;
    private Double exitPrice;
    private Double quantity;
    private Double profitLoss;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String status;
}

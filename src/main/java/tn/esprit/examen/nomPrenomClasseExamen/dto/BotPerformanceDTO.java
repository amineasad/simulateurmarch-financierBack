package tn.esprit.examen.nomPrenomClasseExamen.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BotPerformanceDTO {
    private Long id;
    private Long botId;
    private Double totalReturn;
    private Double totalProfitLoss;
    private Integer totalTrades;
    private Integer winningTrades;
    private Integer losingTrades;
    private Double winRate;
    private Double maxDrawdown;
    private LocalDateTime lastUpdated;
}

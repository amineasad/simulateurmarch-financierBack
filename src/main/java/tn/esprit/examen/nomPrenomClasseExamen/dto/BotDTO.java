package tn.esprit.examen.nomPrenomClasseExamen.dto;

import lombok.Data;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.StrategyType;
import java.time.LocalDateTime;

@Data
public class BotDTO {
    private Long id;
    private String name;
    private String description;
    private StrategyType strategyType;
    private BotStatus status;
    private Long userId;
    private String configuration;
    private Double investmentAmount;
    private String tradingPair;
    private Double maxPositionSize;
    private Double stopLossPercentage;
    private Double takeProfitPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

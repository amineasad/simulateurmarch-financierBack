package tn.esprit.examen.nomPrenomClasseExamen.dto;

import lombok.Data;
import tn.esprit.examen.nomPrenomClasseExamen.entities.StrategyType;

@Data
public class BotTemplateDTO {
    private Long id;
    private String name;
    private String description;
    private StrategyType strategyType;
    private String defaultConfiguration;
    private Double historicalReturns;
    private Double winRate;
    private Double drawdown;
    private Double sharpeRatio;
    private Integer popularity;
}

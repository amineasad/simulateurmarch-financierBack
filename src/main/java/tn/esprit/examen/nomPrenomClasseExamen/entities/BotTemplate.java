package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bot_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private StrategyType strategyType;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String defaultConfiguration; // JSON string

    private Double historicalReturns;
    private Double winRate;
    private Double drawdown;
    private Double sharpeRatio;
    
    private Integer popularity;
}

package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bot_performance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    private Double totalReturn;
    private Double totalProfitLoss;
    private Integer totalTrades;
    private Integer winningTrades;
    private Integer losingTrades;
    
    private Double winRate;
    private Double maxDrawdown;
    
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}

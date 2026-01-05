package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bot_backtests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotBacktest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String configurationSnapshot; 
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private Double totalReturn;
    private Double maxDrawdown;
    private Integer totalTrades;
    private Double winRate;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String tradeHistoryJson;
    
    private LocalDateTime testDate;
}

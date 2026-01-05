package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private StrategyType strategyType;

    @Enumerated(EnumType.STRING)
    private BotStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String configuration; // JSON string for strategy parameters

    private Double investmentAmount;
    private String tradingPair;
    private Double maxPositionSize;
    
    private Double stopLossPercentage;
    private Double takeProfitPercentage;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Using simple mapping for now, will create BotTrade and BotPerformance next
    @OneToMany(mappedBy = "bot", cascade = CascadeType.ALL)
    private List<BotTrade> trades;
    
    @OneToOne(mappedBy = "bot", cascade = CascadeType.ALL)
    private BotPerformance performance;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = BotStatus.STOPPED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bot_trades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_id")
    private Bot bot;

    private String symbol;
    
    @Enumerated(EnumType.STRING)
    private OrderSide side;

    private Double entryPrice;
    private Double exitPrice;
    private Double quantity;
    private Double profitLoss;
    
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    
    private String status; // OPEN, CLOSED
}

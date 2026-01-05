package tn.esprit.examen.nomPrenomClasseExamen.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponseDTO {
    private Long id;
    private Long sessionId;
    private Long userId;

    private String symbol;
    private String type;   // MARKET / LIMIT
    private String side;   // BUY / SELL

    private Integer quantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;

    private Double price;
    private Double executionPrice;

    private String status;           // PENDING / EXECUTED / REJECTED / CANCELLED
    private String rejectionReason;

    private String orderTime;        // ISO-8601
    private String executionTime;    // ISO-8601 (nullable)
}

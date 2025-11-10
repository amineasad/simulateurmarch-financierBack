// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/dto/PositionDTO.java
package tn.esprit.examen.nomPrenomClasseExamen.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PositionDTO {
    private String symbol;
    private double quantity;
    private double avgPrice;
}

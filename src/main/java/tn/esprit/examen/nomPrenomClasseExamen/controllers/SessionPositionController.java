// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/controllers/SessionPositionController.java
package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.dto.PositionDTO;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionPosition;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.SessionPositionRepository;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class SessionPositionController {

    private final SessionPositionRepository repo;

    /**
     * GET /examen/api/positions/session/{sessionId}/user/{userId}
     * Renvoie les positions (symbol, quantity, avgPrice) pour le user dans la session.
     */
    @GetMapping("/session/{sessionId}/user/{userId}")
    public List<PositionDTO> getUserPositions(
            @PathVariable Long sessionId,
            @PathVariable Long userId
    ) {
        List<SessionPosition> rows = repo.findBySession_IdAndUser_Id(sessionId, userId);
        return rows.stream().map(p ->
                PositionDTO.builder()
                        .symbol(p.getSymbol())
                        .quantity(p.getQuantity())
                        .avgPrice(p.getAvgPrice())
                        .build()
        ).toList();
    }
}

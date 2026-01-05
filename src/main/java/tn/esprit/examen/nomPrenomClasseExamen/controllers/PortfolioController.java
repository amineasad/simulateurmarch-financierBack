package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.dto.AllocationRequestDTO;
import tn.esprit.examen.nomPrenomClasseExamen.dto.AllocationResultDTO;
import tn.esprit.examen.nomPrenomClasseExamen.services.PortfolioAllocationService;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioAllocationService allocationService;

    /**
     * POST /api/portfolio/allocate
     *
     * Body :
     * {
     *    "sessionId": 1,
     *    "totalCapital": 10000,
     *    "symbols": ["AAPL","MSFT","SPY"],
     *    "strategy": "MAX_RETURN" // ou "LOW_VOL"
     * }
     */
    @PostMapping("/allocate")
    public AllocationResultDTO allocate(@RequestBody AllocationRequestDTO request) {
        log.info("📊 Requête d'allocation : strategy={}, capital={}, symbols={}",
                request.strategy(), request.totalCapital(), request.symbols());

        return allocationService.allocate(request);
    }
}

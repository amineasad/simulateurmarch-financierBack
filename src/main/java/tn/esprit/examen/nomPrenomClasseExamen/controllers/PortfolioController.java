package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Portfolio;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.PortfolioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Portefeuilles", description = "API de gestion des portefeuilles utilisateur")
public class PortfolioController {

    private final PortfolioRepository portfolioRepository;

    @Operation(summary = "Obtenir un portefeuille", description = "Récupère le portefeuille d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portefeuille trouvé"),
            @ApiResponse(responseCode = "404", description = "Portefeuille non trouvé")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<Portfolio> getPortfolio(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long userId) {
        Optional<Portfolio> portfolio = portfolioRepository.findById(userId);
        return portfolio.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Créer un portefeuille", description = "Crée un nouveau portefeuille pour un utilisateur")
    @ApiResponse(responseCode = "200", description = "Portefeuille créé avec succès")
    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(
            @Parameter(description = "ID de l'utilisateur") @RequestParam Long userId,
            @Parameter(description = "Montant initial en cash") @RequestParam BigDecimal initialCash) {
        Portfolio portfolio = new Portfolio(userId, initialCash);
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return ResponseEntity.ok(savedPortfolio);
    }

    @Operation(summary = "Mettre à jour le cash", description = "Met à jour le montant de cash d'un portefeuille")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cash mis à jour avec succès"),
            @ApiResponse(responseCode = "404", description = "Portefeuille non trouvé")
    })
    @PutMapping("/{userId}/cash")
    public ResponseEntity<Portfolio> updateCash(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long userId,
            @Parameter(description = "Nouveau montant de cash") @RequestParam BigDecimal newCashAmount) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(userId);
        if (portfolioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Portfolio portfolio = portfolioOpt.get();
        portfolio.setCash(newCashAmount);
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return ResponseEntity.ok(savedPortfolio);
    }
}

package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Asset;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.AssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Actifs", description = "API de gestion des actifs financiers")
public class AssetController {

    private final AssetRepository assetRepository;

    @Operation(summary = "Obtenir tous les actifs", description = "Récupère la liste de tous les actifs disponibles")
    @ApiResponse(responseCode = "200", description = "Liste des actifs récupérée avec succès")
    @GetMapping
    public ResponseEntity<List<Asset>> getAllAssets() {
        List<Asset> assets = assetRepository.findAll();
        return ResponseEntity.ok(assets);
    }

    @Operation(summary = "Obtenir un actif par ID", description = "Récupère un actif spécifique par son identifiant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Actif trouvé"),
            @ApiResponse(responseCode = "404", description = "Actif non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Asset> getAsset(
            @Parameter(description = "ID de l'actif") @PathVariable Long id) {
        Optional<Asset> asset = assetRepository.findById(id);
        return asset.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtenir un actif par symbole", description = "Récupère un actif par son symbole (ex: AAPL)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Actif trouvé"),
            @ApiResponse(responseCode = "404", description = "Actif non trouvé")
    })
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<Asset> getAssetBySymbol(
            @Parameter(description = "Symbole de l'actif") @PathVariable String symbol) {
        Optional<Asset> asset = assetRepository.findBySymbol(symbol);
        return asset.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Créer un actif", description = "Crée un nouvel actif financier")
    @ApiResponse(responseCode = "200", description = "Actif créé avec succès")
    @PostMapping
    public ResponseEntity<Asset> createAsset(
            @Parameter(description = "Données de l'actif à créer") @RequestBody Asset asset) {
        Asset savedAsset = assetRepository.save(asset);
        return ResponseEntity.ok(savedAsset);
    }
}

package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import tn.esprit.examen.nomPrenomClasseExamen.DTO.*;
import tn.esprit.examen.nomPrenomClasseExamen.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Ordres", description = "API de gestion des ordres de trading")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Créer un ordre", description = "Crée un nouvel ordre d'achat ou de vente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ordre créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping
    public ResponseEntity<OrderView> createOrder(
            @Parameter(description = "Données de l'ordre à créer") @RequestBody CreateOrderDTO dto,
            @Parameter(description = "ID de l'utilisateur") @RequestParam Long userId) {
        try {
            OrderView orderView = orderService.placeOrder(dto, userId);
            return ResponseEntity.ok(orderView);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Annuler un ordre", description = "Annule un ordre existant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ordre annulé avec succès"),
            @ApiResponse(responseCode = "404", description = "Ordre non trouvé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "ID de l'ordre à annuler") @PathVariable Long id,
            @Parameter(description = "ID de l'utilisateur") @RequestParam Long userId) {
        try {
            orderService.cancelOrder(id, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Obtenir les ordres d'un utilisateur", description = "Récupère tous les ordres d'un utilisateur")
    @ApiResponse(responseCode = "200", description = "Liste des ordres récupérée avec succès")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderView>> getUserOrders(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long userId) {
        List<OrderView> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Obtenir les transactions d'un utilisateur", description = "Récupère toutes les transactions d'un utilisateur")
    @ApiResponse(responseCode = "200", description = "Liste des transactions récupérée avec succès")
    @GetMapping("/user/{userId}/trades")
    public ResponseEntity<List<TradeView>> getUserTrades(
            @Parameter(description = "ID de l'utilisateur") @PathVariable Long userId) {
        List<TradeView> trades = orderService.getUserTrades(userId);
        return ResponseEntity.ok(trades);
    }

    @Operation(summary = "Obtenir le carnet d'ordres", description = "Récupère le carnet d'ordres d'un actif")
    @ApiResponse(responseCode = "200", description = "Carnet d'ordres récupéré avec succès")
    @GetMapping("/orderbook/{assetId}")
    public ResponseEntity<OrderBookSnapshot> getOrderBook(
            @Parameter(description = "ID de l'actif") @PathVariable Long assetId) {
        OrderBookSnapshot snapshot = orderService.getOrderBookSnapshot(assetId);
        return ResponseEntity.ok(snapshot);
    }
}

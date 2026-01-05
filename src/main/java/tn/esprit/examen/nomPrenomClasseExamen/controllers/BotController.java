package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.dto.BotDTO;
import tn.esprit.examen.nomPrenomClasseExamen.dto.BotTemplateDTO;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.StrategyType;
import tn.esprit.examen.nomPrenomClasseExamen.services.BotMarketplaceService;
import tn.esprit.examen.nomPrenomClasseExamen.services.BotService;

import java.util.List;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BotController {

    private final BotService botService;
    private final BotMarketplaceService botMarketplaceService;

    // --- Bot Management ---

    @PostMapping
    public ResponseEntity<BotDTO> createBot(@RequestBody BotDTO botDTO) {
        return ResponseEntity.ok(botService.createBot(botDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BotDTO> updateBot(@PathVariable Long id, @RequestBody BotDTO botDTO) {
        return ResponseEntity.ok(botService.updateBot(id, botDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BotDTO> getBot(@PathVariable Long id) {
        return ResponseEntity.ok(botService.getBot(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BotDTO>> getUserBots(@PathVariable Long userId) {
        return ResponseEntity.ok(botService.getUserBots(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBot(@PathVariable Long id) {
        botService.deleteBot(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BotDTO> updateBotStatus(@PathVariable Long id, @RequestParam BotStatus status) {
        return ResponseEntity.ok(botService.updateBotStatus(id, status));
    }

    // --- Marketplace ---

    @GetMapping("/marketplace")
    public ResponseEntity<List<BotTemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(botMarketplaceService.getAllTemplates());
    }

    @GetMapping("/marketplace/strategy/{strategyType}")
    public ResponseEntity<List<BotTemplateDTO>> getTemplatesByStrategy(@PathVariable StrategyType strategyType) {
        return ResponseEntity.ok(botMarketplaceService.getTemplatesByStrategy(strategyType));
    }
    
    @PostMapping("/marketplace")
    public ResponseEntity<BotTemplateDTO> createTemplate(@RequestBody BotTemplateDTO templateDTO) {
        return ResponseEntity.ok(botMarketplaceService.createTemplate(templateDTO));
    }
}

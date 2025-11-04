package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.UserMarketControl;
import tn.esprit.examen.nomPrenomClasseExamen.entities.UserTradingLimits;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserMarketControlRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserTradingLimitsRepository;
import tn.esprit.examen.nomPrenomClasseExamen.services.userbook.UserBookService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdminController {

    private final UserTradingLimitsRepository limitsRepository;
    private final UserMarketControlRepository controlRepository;
    private final UserBookService userBookService;

    @PutMapping("/limits/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setGlobalLimits(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        UserTradingLimits limits = new UserTradingLimits();
        limits.setUserId(userId);
        limits.setAssetId(null);
        applyLimitsFromBody(limits, body);
        limits.setUpdatedAt(Instant.now());
        limitsRepository.save(limits);
        return ok("limits.updated", null);
    }

    @PutMapping("/limits/{userId}/{assetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setAssetLimits(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestBody Map<String, Object> body) {
        UserTradingLimits limits = limitsRepository.findByUserIdAndAssetId(userId, assetId)
                .orElseGet(UserTradingLimits::new);
        limits.setUserId(userId);
        limits.setAssetId(assetId);
        applyLimitsFromBody(limits, body);
        limits.setUpdatedAt(Instant.now());
        limitsRepository.save(limits);
        return ok("limits.updated", null);
    }

    @PutMapping("/halt/user/{userId}/{assetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> haltUserAsset(
            @PathVariable Long userId,
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "true") boolean halted,
            @RequestParam(required = false) String reason) {
        UserMarketControl ctrl = controlRepository.findByUserIdAndAssetId(userId, assetId)
                .orElseGet(UserMarketControl::new);
        ctrl.setUserId(userId);
        ctrl.setAssetId(assetId);
        ctrl.setState(halted ? UserMarketControl.State.HALTED_USER : UserMarketControl.State.OPEN);
        ctrl.setReason(reason);
        controlRepository.save(ctrl);
        Map<String, Object> state = new HashMap<>();
        state.put("state", ctrl.getState().name());
        state.put("reason", ctrl.getReason());
        state.put("ts", Instant.now().toString());
        return ResponseEntity.ok(state);
    }

    @PostMapping("/cancelAll/{userId}/{assetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelAll(
            @PathVariable Long userId,
            @PathVariable Long assetId) {
        // Cette implémentation est un placeholder; l'annulation en masse doit être branchée au OrderService
        // pour libérer les réserves et publier les snapshots
        return ok("cancel.queued", null);
    }

    @GetMapping("/userbook/{userId}/{assetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserBook(
            @PathVariable Long userId,
            @PathVariable Long assetId) {
        return ResponseEntity.ok(userBookService.getSnapshot(userId, assetId));
    }

    private void applyLimitsFromBody(UserTradingLimits limits, Map<String, Object> body) {
        if (body.containsKey("maxDailyNotional")) {
            limits.setMaxDailyNotional(new BigDecimal(body.get("maxDailyNotional").toString()));
        }
        if (body.containsKey("maxOrderSize")) {
            limits.setMaxOrderSize(Integer.parseInt(body.get("maxOrderSize").toString()));
        }
        if (body.containsKey("maxOpenOrders")) {
            limits.setMaxOpenOrders(Integer.parseInt(body.get("maxOpenOrders").toString()));
        }
        if (body.containsKey("blocked")) {
            limits.setBlocked(Boolean.parseBoolean(body.get("blocked").toString()));
        }
    }

    private ResponseEntity<?> ok(String reason, Object data) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("reason", reason);
        resp.put("ts", Instant.now().toString());
        if (data != null) resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
}


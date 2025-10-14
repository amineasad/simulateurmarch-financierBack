package tn.esprit.examen.nomPrenomClasseExamen.controllers;

import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.examen.nomPrenomClasseExamen.entities.WalletTransaction;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Wallet;
import tn.esprit.examen.nomPrenomClasseExamen.services.StripeService;
import tn.esprit.examen.nomPrenomClasseExamen.services.WalletService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin("*")
public class WalletController {

    private final WalletService walletService;
    private final StripeService stripeService;

    public WalletController(WalletService walletService, StripeService stripeService) {
        this.walletService = walletService;
        this.stripeService = stripeService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long userId) {
        try {
            Wallet wallet = walletService.getWalletByUserId(userId);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // NOUVEAU: Créer une session Stripe Checkout
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.parseLong(request.get("userId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());

            Map<String, String> response = stripeService.createCheckoutSession(userId, amount);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Erreur Stripe: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.parseLong(request.get("userId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());
            String stripePaymentId = request.get("stripePaymentId").toString();

            WalletTransaction transaction = walletService.deposit(userId, amount, stripePaymentId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.parseLong(request.get("userId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());

            WalletTransaction transaction = walletService.withdraw(userId, amount);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/transactions/{userId}")
    public ResponseEntity<List<WalletTransaction>> getTransactions(@PathVariable Long userId) {
        try {
            List<WalletTransaction> transactions = walletService.getTransactionHistory(userId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
package tn.esprit.examen.nomPrenomClasseExamen.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.WalletTransaction;
import tn.esprit.examen.nomPrenomClasseExamen.entities.User;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Wallet;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.WalletTransactionRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.WalletRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Wallet createWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("Le portefeuille existe déjà");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(0.0);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());

        return walletRepository.save(wallet);
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> createWallet(userId));
    }

    @Transactional
    public WalletTransaction deposit(Long userId, Double amount, String stripePaymentId) {
        if (amount <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }

        Wallet wallet = getWalletByUserId(userId);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(WalletTransaction.TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setStatus(WalletTransaction.TransactionStatus.COMPLETED);
        transaction.setStripePaymentId(stripePaymentId);
        transaction.setCreatedAt(LocalDateTime.now());

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
        return walletTransactionRepository.save(transaction);
    }

    @Transactional
    public WalletTransaction withdraw(Long userId, Double amount) {
        if (amount <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }

        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Solde insuffisant");
        }

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(WalletTransaction.TransactionType.WITHDRAWAL);
        transaction.setAmount(amount);
        transaction.setStatus(WalletTransaction.TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
        return walletTransactionRepository.save(transaction);
    }

    public List<WalletTransaction> getTransactionHistory(Long userId) {
        Wallet wallet = getWalletByUserId(userId);
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
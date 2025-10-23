package tn.esprit.examen.nomPrenomClasseExamen.services;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Asset;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Portfolio;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.AssetRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DataInitializationService implements CommandLineRunner {

    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;

    @Override
    public void run(String... args) throws Exception {
        // Créer quelques actifs de test
        if (assetRepository.count() == 0) {
            Asset apple = new Asset("AAPL", "Apple Inc.");
            Asset google = new Asset("GOOGL", "Alphabet Inc.");
            Asset microsoft = new Asset("MSFT", "Microsoft Corporation");
            Asset tesla = new Asset("TSLA", "Tesla Inc.");

            assetRepository.save(apple);
            assetRepository.save(google);
            assetRepository.save(microsoft);
            assetRepository.save(tesla);

            System.out.println("Actifs créés : AAPL, GOOGL, MSFT, TSLA");
        }

        // Créer quelques portefeuilles de test
        if (portfolioRepository.count() == 0) {
            Portfolio user1 = new Portfolio(1L, new BigDecimal("10000.00"));
            Portfolio user2 = new Portfolio(2L, new BigDecimal("5000.00"));
            Portfolio user3 = new Portfolio(3L, new BigDecimal("15000.00"));

            // Ajouter quelques positions pour user1
            user1.getPositions().put(1L, 10); // 10 AAPL
            user1.getPositions().put(2L, 5);  // 5 GOOGL

            // Ajouter quelques positions pour user2
            user2.getPositions().put(3L, 20); // 20 MSFT
            user2.getPositions().put(4L, 3);  // 3 TSLA

            portfolioRepository.save(user1);
            portfolioRepository.save(user2);
            portfolioRepository.save(user3);

            System.out.println("Portefeuilles créés pour les utilisateurs 1, 2, et 3");
        }
    }
}

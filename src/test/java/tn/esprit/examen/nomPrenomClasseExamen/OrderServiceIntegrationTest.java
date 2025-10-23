package tn.esprit.examen.nomPrenomClasseExamen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.DTO.CreateOrderDTO;
import tn.esprit.examen.nomPrenomClasseExamen.DTO.OrderView;
import tn.esprit.examen.nomPrenomClasseExamen.entities.*;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.*;
import tn.esprit.examen.nomPrenomClasseExamen.services.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour le service d'ordres
 * Vérifie l'atomicité des transactions et la cohérence des données
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;

    private Asset testAsset;
    private User user1, user2;
    private Portfolio portfolio1, portfolio2;

    @BeforeEach
    void setUp() {
        // Créer un actif de test
        testAsset = new Asset();
        testAsset.setSymbol("TEST");
        testAsset.setName("Test Asset");
        testAsset.setCurrentPrice(BigDecimal.valueOf(100.0));
        testAsset = assetRepository.save(testAsset);

        // Créer des utilisateurs
        user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@test.com");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@test.com");
        user2 = userRepository.save(user2);

        // Créer des portefeuilles
        portfolio1 = new Portfolio();
        portfolio1.setUserId(user1.getId());
        portfolio1.setCash(BigDecimal.valueOf(1000.0));
        portfolio1.setReservedCash(BigDecimal.ZERO);
        portfolio1.setPositions(Map.of());
        portfolio1.setReserved(Map.of());
        portfolio1 = portfolioRepository.save(portfolio1);

        portfolio2 = new Portfolio();
        portfolio2.setUserId(user2.getId());
        portfolio2.setCash(BigDecimal.valueOf(1000.0));
        portfolio2.setReservedCash(BigDecimal.ZERO);
        portfolio2.setPositions(Map.of(testAsset.getId(), 10));
        portfolio2.setReserved(Map.of());
        portfolio2 = portfolioRepository.save(portfolio2);
    }

    @Test
    void testInsufficientFundsBuyLimit_ShouldRejectAndNoReservation() {
        // Given: Ordre d'achat avec fonds insuffisants
        CreateOrderDTO dto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(200.0), // Prix élevé
                5 // Quantité
        );

        // When: Placement de l'ordre
        OrderView result = orderService.placeOrder(dto, user1.getId());

        // Then: Ordre rejeté et aucune réservation
        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.error()).contains("Fonds insuffisants");

        // Vérifier qu'aucune réservation n'a été faite
        Portfolio portfolio = portfolioRepository.findById(user1.getId()).orElseThrow();
        assertThat(portfolio.getReservedCash()).isEqualTo(BigDecimal.ZERO);
        assertThat(portfolio.getCash()).isEqualTo(BigDecimal.valueOf(1000.0));
    }

    @Test
    void testInsufficientQuantitySellLimit_ShouldRejectAndNoReservation() {
        // Given: Ordre de vente avec quantité insuffisante
        CreateOrderDTO dto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100.0),
                15 // Plus que disponible (10)
        );

        // When: Placement de l'ordre
        OrderView result = orderService.placeOrder(dto, user2.getId());

        // Then: Ordre rejeté et aucune réservation
        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.error()).contains("Quantité insuffisante");

        // Vérifier qu'aucune réservation n'a été faite
        Portfolio portfolio = portfolioRepository.findById(user2.getId()).orElseThrow();
        assertThat(portfolio.getReserved()).isEmpty();
        assertThat(portfolio.getPositions().get(testAsset.getId())).isEqualTo(10);
    }

    @Test
    void testPartialFillThenCancel_ShouldReflectExecutedQuantityAndReleaseRemainingReservation() {
        // Given: Deux ordres qui vont partiellement matcher
        CreateOrderDTO buyDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100.0),
                3 // Quantité d'achat
        );

        CreateOrderDTO sellDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100.0),
                5 // Quantité de vente (plus que l'achat)
        );

        // When: Placement des ordres
        OrderView buyOrder = orderService.placeOrder(buyDto, user1.getId());
        OrderView sellOrder = orderService.placeOrder(sellDto, user2.getId());

        // Then: Les ordres devraient matcher partiellement
        assertThat(buyOrder.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(sellOrder.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);

        // Vérifier les portefeuilles après matching
        Portfolio buyerPortfolio = portfolioRepository.findById(user1.getId()).orElseThrow();
        Portfolio sellerPortfolio = portfolioRepository.findById(user2.getId()).orElseThrow();

        // Acheteur: 3 actions, 700€ de cash (1000 - 300)
        assertThat(buyerPortfolio.getPositions().get(testAsset.getId())).isEqualTo(3);
        assertThat(buyerPortfolio.getCash()).isEqualTo(BigDecimal.valueOf(700.0));
        assertThat(buyerPortfolio.getReservedCash()).isEqualTo(BigDecimal.ZERO);

        // Vendeur: 7 actions restantes (10 - 3), 1300€ de cash (1000 + 300)
        assertThat(sellerPortfolio.getPositions().get(testAsset.getId())).isEqualTo(7);
        assertThat(sellerPortfolio.getCash()).isEqualTo(BigDecimal.valueOf(1300.0));
        assertThat(sellerPortfolio.getReserved().get(testAsset.getId())).isEqualTo(2); // 5 - 3

        // When: Annulation de l'ordre de vente partiellement exécuté
        orderService.cancelOrder(sellOrder.id(), user2.getId());

        // Then: Libération de la réservation restante
        sellerPortfolio = portfolioRepository.findById(user2.getId()).orElseThrow();
        assertThat(sellerPortfolio.getReserved()).isEmpty(); // Plus de réservation
        assertThat(sellerPortfolio.getPositions().get(testAsset.getId())).isEqualTo(7); // Position inchangée
    }

    @Test
    void testFifoAtSamePrice_ShouldServeOldestOrderFirst() {
        // Given: Deux ordres d'achat au même prix
        CreateOrderDTO firstBuyDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100.0),
                2
        );

        CreateOrderDTO secondBuyDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100.0),
                2
        );

        CreateOrderDTO sellDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100.0),
                1 // Une seule action à vendre
        );

        // When: Placement des ordres dans l'ordre
        OrderView firstBuy = orderService.placeOrder(firstBuyDto, user1.getId());
        OrderView secondBuy = orderService.placeOrder(secondBuyDto, user1.getId());
        OrderView sell = orderService.placeOrder(sellDto, user2.getId());

        // Then: Le premier ordre d'achat devrait être exécuté (FIFO)
        assertThat(firstBuy.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(secondBuy.status()).isEqualTo(OrderStatus.PENDING);

        // Vérifier qu'un trade a été créé
        List<Trade> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getBuyOrderId()).isEqualTo(firstBuy.id());
        assertThat(trades.get(0).getSellOrderId()).isEqualTo(sell.id());
    }

    @Test
    void testOrderMatchingWithDifferentPrices_ShouldExecuteAtCorrectPrice() {
        // Given: Ordres avec prix différents
        CreateOrderDTO buyDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(105.0), // Prix d'achat plus élevé
                1
        );

        CreateOrderDTO sellDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100.0), // Prix de vente plus bas
                1
        );

        // When: Placement des ordres
        OrderView buyOrder = orderService.placeOrder(buyDto, user1.getId());
        OrderView sellOrder = orderService.placeOrder(sellDto, user2.getId());

        // Then: Les ordres devraient matcher au prix de l'ordre le plus ancien
        assertThat(buyOrder.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(sellOrder.status()).isEqualTo(OrderStatus.FILLED);

        // Vérifier le prix d'exécution
        List<Trade> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getPrice()).isEqualTo(BigDecimal.valueOf(100.0)); // Prix du vendeur (plus ancien)
    }

    @Test
    void testMarketOrderExecution_ShouldExecuteAtBestAvailablePrice() {
        // Given: Ordre de marché contre ordre limite
        CreateOrderDTO marketBuyDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.BUY,
                OrderType.MARKET,
                null, // Prix null pour ordre de marché
                1
        );

        CreateOrderDTO limitSellDto = new CreateOrderDTO(
                testAsset.getId(),
                OrderSide.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(95.0),
                1
        );

        // When: Placement des ordres
        OrderView marketBuy = orderService.placeOrder(marketBuyDto, user1.getId());
        OrderView limitSell = orderService.placeOrder(limitSellDto, user2.getId());

        // Then: L'ordre de marché devrait s'exécuter au prix de l'ordre limite
        assertThat(marketBuy.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(limitSell.status()).isEqualTo(OrderStatus.FILLED);

        // Vérifier le prix d'exécution
        List<Trade> trades = tradeRepository.findAll();
        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getPrice()).isEqualTo(BigDecimal.valueOf(95.0)); // Prix de l'ordre limite
    }
}

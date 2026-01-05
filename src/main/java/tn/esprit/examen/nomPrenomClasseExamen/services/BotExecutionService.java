package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Bot;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotTrade;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.BotRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.BotTradeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotExecutionService {

    private final BotRepository botRepository;
    private final BotTradeRepository botTradeRepository;
    private final Random random = new Random();

    // Run every 5 seconds to simulate high-frequency checks
    @Scheduled(fixedRate = 5000)
    public void executeBots() {
        List<Bot> activeBots = botRepository.findAll().stream()
                .filter(b -> b.getStatus() == BotStatus.ACTIVE)
                .toList();

        if (activeBots.isEmpty()) return;

        log.info("Executing logic for {} active bots", activeBots.size());

        for (Bot bot : activeBots) {
            try {
                processBot(bot);
            } catch (Exception e) {
                log.error("Error executing bot {}: {}", bot.getId(), e.getMessage());
            }
        }
    }

    private void processBot(Bot bot) {
        // Placeholder for actual strategy logic
        // In a real app, this would check market data, indicators, etc.
        
        // Simulating a random trade for demonstration
        if (random.nextInt(100) < 5) { // 5% chance to trade per cycle
            executeTrade(bot);
        }
    }

    private void executeTrade(Bot bot) {
        BotTrade trade = new BotTrade();
        trade.setBot(bot);
        trade.setSymbol(bot.getTradingPair());
        trade.setSide(random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL);
        trade.setEntryTime(LocalDateTime.now());
        
        // Simulating price (replace with real market price)
        double currentPrice = 1.05 + (random.nextDouble() * 0.1); 
        trade.setEntryPrice(currentPrice);
        
        trade.setQuantity(bot.getInvestmentAmount() / currentPrice);
        trade.setStatus("OPEN");
        
        botTradeRepository.save(trade);
        log.info("Bot {} executed trade: {} {}", bot.getName(), trade.getSide(), trade.getSymbol());
    }
}

package tn.esprit.examen.nomPrenomClasseExamen.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotTemplate;
import tn.esprit.examen.nomPrenomClasseExamen.entities.StrategyType;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.BotTemplateRepository;

@Component
@RequiredArgsConstructor
public class BotDataInitializer implements CommandLineRunner {

    private final BotTemplateRepository botTemplateRepository;

    @Override
    public void run(String... args) throws Exception {
        if (botTemplateRepository.count() == 0) {
            seedTemplates();
        }
    }

    private void seedTemplates() {
        createTemplate(
            "RSI Scalper",
            "A simple scalping strategy based on RSI overbought/oversold levels.",
            StrategyType.SCALPING,
            "{\"indicator\": \"RSI\", \"period\": 14, \"overbought\": 70, \"oversold\": 30}",
            15.5, 65.0, 5.2, 1.8
        );

        createTemplate(
            "Moving Average Crossover",
            "Classic trend following strategy using SMA 50 and SMA 200.",
            StrategyType.SWING_TRADING,
            "{\"indicator\": \"SMA\", \"fast\": 50, \"slow\": 200}",
            22.4, 55.0, 12.5, 1.5
        );

        createTemplate(
            "BTC DCA Accumulator",
            "Dollar Cost Averaging strategy for Bitcoin accumulation.",
            StrategyType.DCA,
            "{\"asset\": \"BTC\", \"interval\": \"DAILY\", \"amount\": 50}",
            45.0, 100.0, 0.0, 2.1
        );
        
        createTemplate(
            "ETH/USDT Grid Bot",
            "Grid trading strategy for sideways market on ETH.",
            StrategyType.GRID_TRADING,
            "{\"grids\": 10, \"lower\": 2000, \"upper\": 3000}",
            12.0, 80.0, 2.5, 2.5
        );
    }

    private void createTemplate(String name, String description, StrategyType type, String config, 
                                Double returns, Double winRate, Double drawdown, Double sharpe) {
        BotTemplate template = new BotTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setStrategyType(type);
        template.setDefaultConfiguration(config);
        template.setHistoricalReturns(returns);
        template.setWinRate(winRate);
        template.setDrawdown(drawdown);
        template.setSharpeRatio(sharpe);
        template.setPopularity(0);
        botTemplateRepository.save(template);
    }
}

package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotTrade;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Bot;
import java.util.List;

public interface BotTradeRepository extends JpaRepository<BotTrade, Long> {
    List<BotTrade> findByBot(Bot bot);
    List<BotTrade> findByBotId(Long botId);
}

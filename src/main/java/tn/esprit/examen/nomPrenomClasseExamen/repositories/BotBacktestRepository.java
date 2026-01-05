package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotBacktest;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Bot;
import java.util.List;

public interface BotBacktestRepository extends JpaRepository<BotBacktest, Long> {
    List<BotBacktest> findByBot(Bot bot);
}

package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotPerformance;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Bot;
import java.util.Optional;

public interface BotPerformanceRepository extends JpaRepository<BotPerformance, Long> {
    Optional<BotPerformance> findByBot(Bot bot);
}

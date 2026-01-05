package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.BotTemplate;
import tn.esprit.examen.nomPrenomClasseExamen.entities.StrategyType;
import java.util.List;

public interface BotTemplateRepository extends JpaRepository<BotTemplate, Long> {
    List<BotTemplate> findByStrategyType(StrategyType strategyType);
}

package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Bot;
import tn.esprit.examen.nomPrenomClasseExamen.entities.User;
import java.util.List;

public interface BotRepository extends JpaRepository<Bot, Long> {
    List<Bot> findByUser(User user);
    List<Bot> findByUserId(Long userId);
}

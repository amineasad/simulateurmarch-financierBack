// tn.esprit.examen.nomPrenomClasseExamen.repositories.MarketEventRepository

package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.MarketEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MarketEventRepository extends JpaRepository<MarketEvent, Long> {

    List<MarketEvent> findBySessionIdOrderByDeclenchementPrevuAsc(Long sessionId);

    List<MarketEvent> findBySessionIdAndDeclencheTrue(Long sessionId);

    List<MarketEvent> findBySessionIdAndDeclencheFalse(Long sessionId);

    @Query("SELECT e FROM MarketEvent e WHERE e.session.id = :sessionId " +
            "AND e.declenche = false " +
            "AND e.declenchementPrevu <= :now " +
            "ORDER BY e.declenchementPrevu ASC")
    List<MarketEvent> findPendingEvents(@Param("sessionId") Long sessionId, @Param("now") LocalDateTime now);

    Long countBySessionIdAndDeclencheTrue(Long sessionId);
}
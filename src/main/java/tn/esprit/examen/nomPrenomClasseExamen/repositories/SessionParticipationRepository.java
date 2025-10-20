// tn.esprit.examen.nomPrenomClasseExamen.repositories.SessionParticipationRepository

package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionParticipation;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionParticipationRepository extends JpaRepository<SessionParticipation, Long> {

    Optional<SessionParticipation> findBySessionIdAndUserId(Long sessionId, Long userId);

    List<SessionParticipation> findBySessionIdOrderByClassementAsc(Long sessionId);

    List<SessionParticipation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SessionParticipation> findBySessionIdAndConnecteTrue(Long sessionId);

    @Query("SELECT p FROM SessionParticipation p WHERE p.session.id = :sessionId ORDER BY p.rendement DESC")
    List<SessionParticipation> getLeaderboard(@Param("sessionId") Long sessionId);

    @Query("SELECT COUNT(p) FROM SessionParticipation p WHERE p.session.id = :sessionId AND p.connecte = true")
    Long countActiveParticipants(@Param("sessionId") Long sessionId);

    Boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
}
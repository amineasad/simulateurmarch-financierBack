// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/repositories/SessionPositionRepository.java
package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionPosition;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionPositionRepository extends JpaRepository<SessionPosition, Long> {

    // Position spécifique
    @Query("SELECT p FROM SessionPosition p WHERE p.session.id = :sessionId " +
            "AND p.user.id = :userId AND p.symbol = :symbol")
    Optional<SessionPosition> findBySessionIdAndUserIdAndSymbol(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("symbol") String symbol
    );

    // Toutes les positions d'un utilisateur dans une session
    @Query("SELECT p FROM SessionPosition p WHERE p.session.id = :sessionId " +
            "AND p.user.id = :userId")
    List<SessionPosition> findBySessionIdAndUserId(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId
    );

    // Positions avec quantité > 0
    @Query("SELECT p FROM SessionPosition p WHERE p.session.id = :sessionId " +
            "AND p.user.id = :userId AND p.quantity > 0")
    List<SessionPosition> findActivePositions(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId
    );
    // ✅ Pour la liste des positions d’un user dans une session (utilisé par le contrôleur Positions)
    List<SessionPosition> findBySession_IdAndUser_Id(Long sessionId, Long userId);
}
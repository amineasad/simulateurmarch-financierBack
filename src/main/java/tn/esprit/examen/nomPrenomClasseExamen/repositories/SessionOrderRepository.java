// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/repositories/SessionOrderRepository.java
package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionOrder;

import java.util.List;

@Repository
public interface SessionOrderRepository extends JpaRepository<SessionOrder, Long> {

    // Ordres d'une session triés par date
    List<SessionOrder> findBySession_IdOrderByOrderTimeDesc(Long sessionId);

    // Ordres d'un utilisateur dans une session
    List<SessionOrder> findBySession_IdAndUser_IdOrderByOrderTimeDesc(Long sessionId, Long userId);

    // Ordres par statut
    List<SessionOrder> findBySession_IdAndStatusOrderByOrderTimeDesc(Long sessionId, OrderStatus status);

    // Ordres d'un utilisateur par statut
    List<SessionOrder> findBySession_IdAndUser_IdAndStatusOrderByOrderTimeDesc(
            Long sessionId,
            Long userId,
            OrderStatus status
    );

    // Compter les ordres par statut
    long countBySession_IdAndStatus(Long sessionId, OrderStatus status);

    // Ordres récemment exécutés (feed d'activité)
    @Query("SELECT o FROM SessionOrder o WHERE o.session.id = :sessionId " +
            "AND o.status = 'EXECUTED' " +
            "ORDER BY o.executionTime DESC")
    List<SessionOrder> getRecentExecutedOrders(@Param("sessionId") Long sessionId);

    // Volume total tradé
    @Query("SELECT SUM(o.filledQuantity * o.executionPrice) FROM SessionOrder o " +
            "WHERE o.session.id = :sessionId AND o.status = 'EXECUTED'")
    Double calculateTotalVolume(@Param("sessionId") Long sessionId);

    // Ordres d'un symbole
    List<SessionOrder> findBySession_IdAndSymbolOrderByOrderTimeDesc(Long sessionId, String symbol);
}
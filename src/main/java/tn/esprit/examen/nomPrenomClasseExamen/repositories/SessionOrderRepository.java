// tn.esprit.examen.nomPrenomClasseExamen.repositories.SessionOrderRepository

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

    List<SessionOrder> findBySessionIdOrderByOrderTimeDesc(Long sessionId);

    List<SessionOrder> findBySessionIdAndUserIdOrderByOrderTimeDesc(Long sessionId, Long userId);

    List<SessionOrder> findBySessionIdAndSymbolOrderByOrderTimeDesc(Long sessionId, String symbol);

    List<SessionOrder> findBySessionIdAndStatusOrderByOrderTimeAsc(Long sessionId, OrderStatus status);

    Long countBySessionIdAndUserId(Long sessionId, Long userId);

    @Query("SELECT SUM(o.price * o.quantity) FROM SessionOrder o " +
            "WHERE o.session.id = :sessionId AND o.user.id = :userId AND o.status = 'EXECUTED'")
    Double calculateUserVolume(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    @Query("SELECT o FROM SessionOrder o WHERE o.session.id = :sessionId " +
            "AND o.status = 'EXECUTED' ORDER BY o.executionTime DESC")
    List<SessionOrder> getRecentExecutedOrders(@Param("sessionId") Long sessionId);

    @Query("SELECT SUM(o.price * o.quantity) FROM SessionOrder o " +
            "WHERE o.session.id = :sessionId AND o.status = 'EXECUTED'")
    Double calculateTotalVolume(@Param("sessionId") Long sessionId);
}
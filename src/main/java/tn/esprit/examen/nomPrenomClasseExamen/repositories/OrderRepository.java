package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Order;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide; // Assurez-vous que cette importation est présente
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    List<Order> findByAssetIdAndStatus(Long assetId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.asset.id = :assetId AND o.status IN (tn.esprit.examen.nomPrenomClasseExamen.entities.OrderStatus.PENDING, tn.esprit.examen.nomPrenomClasseExamen.entities.OrderStatus.EXECUTED) ORDER BY " +
            "CASE o.OrderSide WHEN tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide.BUY THEN o.price END DESC, " +
            "CASE o.OrderSide WHEN tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide.SELL THEN o.price END ASC, " +
            "o.createdAt ASC")
    List<Order> findActiveOrdersByAsset(@Param("assetId") Long assetId);
}
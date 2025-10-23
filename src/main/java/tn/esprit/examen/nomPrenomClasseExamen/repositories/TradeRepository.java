package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByAssetIdOrderByExecutedAtDesc(Long assetId);

    List<Trade> findByBuyOrderIdOrSellOrderId(Long buyOrderId, Long sellOrderId);

    @Query("SELECT t FROM Trade t WHERE t.assetId = :assetId AND t.executedAt >= :fromDate ORDER BY t.executedAt DESC")
    List<Trade> findByAssetIdAndExecutedAtAfter(@Param("assetId") Long assetId, @Param("fromDate") Instant fromDate);

    @Query("SELECT t FROM Trade t WHERE (t.buyOrderId = :orderId OR t.sellOrderId = :orderId) ORDER BY t.executedAt DESC")
    List<Trade> findByOrderId(@Param("orderId") Long orderId);
}

package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.examen.nomPrenomClasseExamen.entities.UserTradingLimits;

import java.util.List;
import java.util.Optional;

public interface UserTradingLimitsRepository extends JpaRepository<UserTradingLimits, Long> {

    @Query("SELECT l FROM UserTradingLimits l WHERE l.userId = :userId AND (l.assetId IS NULL OR l.assetId = :assetId) ORDER BY l.assetId DESC")
    List<UserTradingLimits> findEffectiveLimits(Long userId, Long assetId);

    Optional<UserTradingLimits> findByUserIdAndAssetId(Long userId, Long assetId);
}


package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.UserMarketControl;

import java.util.Optional;

public interface UserMarketControlRepository extends JpaRepository<UserMarketControl, Long> {
    Optional<UserMarketControl> findByUserIdAndAssetId(Long userId, Long assetId);
}


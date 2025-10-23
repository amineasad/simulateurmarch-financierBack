package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);
}

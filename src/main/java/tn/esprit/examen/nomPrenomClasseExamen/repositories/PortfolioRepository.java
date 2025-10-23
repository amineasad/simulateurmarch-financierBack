package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import tn.esprit.examen.nomPrenomClasseExamen.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}

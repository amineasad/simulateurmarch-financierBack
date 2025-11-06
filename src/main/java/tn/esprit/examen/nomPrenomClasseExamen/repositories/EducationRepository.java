package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.EducationResource;

@Repository
public interface EducationRepository extends JpaRepository<EducationResource, Long> {
    // Additional query methods can be added if needed
}

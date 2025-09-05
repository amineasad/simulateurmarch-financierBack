package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Reponse;

public interface ReponseRepository extends JpaRepository<Reponse, Long> {
    Reponse findByQuestionId(Long questionId);
}

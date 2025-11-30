package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.ForumPost;

import java.util.List;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    List<ForumPost> findAllByOrderByCreatedAtDesc();}

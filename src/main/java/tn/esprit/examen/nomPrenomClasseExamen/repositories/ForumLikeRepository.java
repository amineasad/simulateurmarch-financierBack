package tn.esprit.examen.nomPrenomClasseExamen.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.examen.nomPrenomClasseExamen.entities.ForumLike;

import java.util.Optional;

public interface ForumLikeRepository extends JpaRepository<ForumLike, Long> {
    Optional<ForumLike> findByUserIdAndPostId(Long userId, Long postId);
}

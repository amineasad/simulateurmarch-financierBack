package tn.esprit.examen.nomPrenomClasseExamen.entities;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "forum_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ForumLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;
}

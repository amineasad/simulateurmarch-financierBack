package tn.esprit.examen.nomPrenomClasseExamen.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    // Type de profil
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileType profileType;

    // Champs communs
    @Column(length = 20)
    private String cin;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoVisage;

    // Champs spécifiques ÉTUDIANT
    @Column(length = 50)
    private String carteEtudiant;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoFace;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoProfil;

    // Champs spécifiques ENTREPRISE
    private String nomEntreprise;
    private String matriculeFiscale;
    private String adresseEntreprise;
    private String secteurActivite;
    private String numeroRegistreCommerce;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String documentLegal;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== ✅ RELATIONS FORUM (AJOUTÉES) ==========

    /**
     * Posts créés par cet utilisateur
     */
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ForumPost> forumPosts = new ArrayList<>();

    /**
     * Commentaires créés par cet utilisateur
     */
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ForumComment> forumComments = new ArrayList<>();

    /**
     * Likes donnés par cet utilisateur
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ForumLike> forumLikes = new ArrayList<>();

    // ========== MÉTHODES ==========

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Nom complet
     */
    public String getFullName() {
        return nom + " " + prenom;
    }

    /**
     * Photo de profil avec fallback
     */
    public String getProfilePhotoOrDefault() {
        if (photoProfil != null && !photoProfil.isEmpty()) {
            return photoProfil;
        }
        if (photoFace != null && !photoFace.isEmpty()) {
            return photoFace;
        }
        if (photoVisage != null && !photoVisage.isEmpty()) {
            return photoVisage;
        }
        return "https://via.placeholder.com/150";
    }
}
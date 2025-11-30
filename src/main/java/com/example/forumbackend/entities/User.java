package com.example.forumbackend.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
    private ProfileType profileType; // STUDENT, COMPANY, INDIVIDUAL

    // Champs communs
    @Column(length = 20)
    private String cin;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoVisage; // Base64

    // Champs spécifiques ÉTUDIANT
    @Column(length = 50)
    private String carteEtudiant;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoFace; // Photo de face (Base64)

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoProfil; // Photo de profil (Base64)

    // Champs spécifiques ENTREPRISE
    private String nomEntreprise;
    private String matriculeFiscale;
    private String adresseEntreprise;
    private String secteurActivite;
    private String numeroRegistreCommerce;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String documentLegal;

    // Date de création
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

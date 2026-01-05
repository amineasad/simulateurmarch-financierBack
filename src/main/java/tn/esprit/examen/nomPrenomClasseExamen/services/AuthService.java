

package tn.esprit.examen.nomPrenomClasseExamen.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.User;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * ✅ MODIFIÉ : Enregistrement avec validation selon le type de profil
     */
    @Transactional
    public User register(User user) {
        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Validation des champs obligatoires selon le profil
        validateProfileRequirements(user);

        // Crypter le mot de passe
        user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));

        // Sauvegarder l'utilisateur
        return userRepository.save(user);
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Validation selon le type de profil
     */
    private void validateProfileRequirements(User user) {
        if (user.getProfileType() == null) {
            throw new RuntimeException("Le type de profil est obligatoire");
        }

        switch (user.getProfileType()) {
            case STUDENT:
                if (user.getCin() == null || user.getCin().isBlank()) {
                    throw new RuntimeException("Le CIN est obligatoire pour les étudiants");
                }
                if (user.getCarteEtudiant() == null || user.getCarteEtudiant().isBlank()) {
                    throw new RuntimeException("La carte d'étudiant est obligatoire");
                }
                if (user.getPhotoFace() == null || user.getPhotoFace().isBlank()) {
                    throw new RuntimeException("La photo de face est obligatoire");
                }
                if (user.getPhotoProfil() == null || user.getPhotoProfil().isBlank()) {
                    throw new RuntimeException("La photo de profil est obligatoire");
                }
                break;

            case COMPANY:
                if (user.getNomEntreprise() == null || user.getNomEntreprise().isBlank()) {
                    throw new RuntimeException("Le nom de l'entreprise est obligatoire");
                }
                if (user.getMatriculeFiscale() == null || user.getMatriculeFiscale().isBlank()) {
                    throw new RuntimeException("Le matricule fiscale est obligatoire");
                }
                if (user.getNumeroRegistreCommerce() == null || user.getNumeroRegistreCommerce().isBlank()) {
                    throw new RuntimeException("Le numéro de registre de commerce est obligatoire");
                }
                break;

            case INDIVIDUAL:
                if (user.getCin() == null || user.getCin().isBlank()) {
                    throw new RuntimeException("Le CIN est obligatoire");
                }
                if (user.getPhotoVisage() == null || user.getPhotoVisage().isBlank()) {
                    throw new RuntimeException("La photo de visage est obligatoire");
                }
                break;
        }
    }

    /**
     * Login (pas de changement)
     */
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(password, user.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        return user;
    }
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + id));
    }
}
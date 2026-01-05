package tn.esprit.examen.nomPrenomClasseExamen.dto;
import jakarta.validation.constraints.NotBlank;
public record CreatePostRequest(
        @NotBlank(message = "Le contenu est obligatoire")
        String content

) {}

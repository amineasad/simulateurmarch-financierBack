package tn.esprit.examen.nomPrenomClasseExamen.dto;

import java.util.List;

/**
 * Requête d'allocation de portefeuille.
 */
public record AllocationRequestDTO(
        Long sessionId,
        double totalCapital,          // montant total à allouer (ex : 10.000$)
        List<String> symbols,         // liste des actifs sélectionnés
        String strategy               // "MAX_RETURN" ou "LOW_VOL"
) {}

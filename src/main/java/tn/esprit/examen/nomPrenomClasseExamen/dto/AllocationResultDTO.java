package tn.esprit.examen.nomPrenomClasseExamen.dto;

import java.util.Map;

/**
 * Résultat d'allocation :
 * - pondération cible
 * - montants par actif
 * - petites stats.
 */
public record AllocationResultDTO(
        String strategy,
        double totalCapital,
        Map<String, Double> weights,       // symbole -> poids (0..1)
        Map<String, Double> amounts,       // symbole -> montant alloué
        Map<String, Double> expectedReturn,// symbole -> rendement simple estimé
        Map<String, Double> volatility,    // symbole -> volatilité estimée
        String comment                     // petit texte explicatif
) {}

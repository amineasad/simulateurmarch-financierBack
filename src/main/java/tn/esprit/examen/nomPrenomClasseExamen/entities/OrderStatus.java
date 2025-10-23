package tn.esprit.examen.nomPrenomClasseExamen.entities;

public enum OrderStatus {
    NEW,              // Nouvel ordre créé
    VALIDATING,       // En cours de validation
    QUEUED,           // En file d'attente
    PENDING,          // En attente de matching
    PARTIALLY_FILLED, // Partiellement exécuté
    FILLED,           // Complètement exécuté
    EXECUTED,         // Exécuté (alias pour FILLED)
    CANCELLED,        // Annulé
    REJECTED          // Rejeté
}

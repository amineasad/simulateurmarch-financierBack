package tn.esprit.examen.nomPrenomClasseExamen.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement de domaine pour la publication post-commit
 * Garantit la publication exactly-once après validation de la transaction
 */
public record DomainEvent(
        String id,
        String type,
        Object payload,
        Instant timestamp,
        String userId,
        String assetId
) {
    
    public static DomainEvent of(String type, Object payload) {
        return new DomainEvent(
                UUID.randomUUID().toString(),
                type,
                payload,
                Instant.now(),
                null,
                null
        );
    }
    
    public static DomainEvent of(String type, Object payload, String userId) {
        return new DomainEvent(
                UUID.randomUUID().toString(),
                type,
                payload,
                Instant.now(),
                userId,
                null
        );
    }
    
    public static DomainEvent of(String type, Object payload, String userId, String assetId) {
        return new DomainEvent(
                UUID.randomUUID().toString(),
                type,
                payload,
                Instant.now(),
                userId,
                assetId
        );
    }
    
    public DomainEvent withUserId(String userId) {
        return new DomainEvent(id, type, payload, timestamp, userId, assetId);
    }
    
    public DomainEvent withAssetId(String assetId) {
        return new DomainEvent(id, type, payload, timestamp, userId, assetId);
    }
}

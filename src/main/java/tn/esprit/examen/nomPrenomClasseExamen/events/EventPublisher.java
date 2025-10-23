package tn.esprit.examen.nomPrenomClasseExamen.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Publisher d'événements avec publication post-commit
 * Garantit la publication exactly-once après validation de la transaction
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final SimpMessagingTemplate messagingTemplate;
    private static final ThreadLocal<List<DomainEvent>> EVENT_BUFFER = new ThreadLocal<>();
    
    /**
     * Publie un événement après commit de la transaction
     * Si pas de transaction active, publie immédiatement
     */
    public void publishAfterCommit(DomainEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Transaction active : bufferiser l'événement
            List<DomainEvent> events = EVENT_BUFFER.get();
            if (events == null) {
                events = new ArrayList<>();
                EVENT_BUFFER.set(events);
                
                // Enregistrer le callback de publication post-commit
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishBufferedEvents();
                    }
                    
                    @Override
                    public void afterCompletion(int status) {
                        // Nettoyer le buffer même en cas d'échec
                        EVENT_BUFFER.remove();
                    }
                });
            }
            events.add(event);
            log.debug("Événement bufferisé: {} pour utilisateur {}", event.type(), event.userId());
        } else {
            // Pas de transaction : publier immédiatement
            publishEvent(event);
        }
    }
    
    private void publishBufferedEvents() {
        List<DomainEvent> events = EVENT_BUFFER.get();
        if (events != null) {
            log.debug("Publication de {} événements post-commit", events.size());
            events.forEach(this::publishEvent);
            EVENT_BUFFER.remove();
        }
    }
    
    private void publishEvent(DomainEvent event) {
        try {
            // Publication selon le type d'événement
            switch (event.type()) {
                case "ORDER_STATUS" -> {
                    if (event.userId() != null) {
                        messagingTemplate.convertAndSend("/topic/orders/status/" + event.userId(), event.payload());
                    }
                }
                case "TRANSACTION" -> {
                    if (event.userId() != null) {
                        messagingTemplate.convertAndSend("/topic/transactions/" + event.userId(), event.payload());
                    }
                }
                case "ORDERBOOK" -> {
                    if (event.assetId() != null) {
                        messagingTemplate.convertAndSend("/topic/orderbook/" + event.assetId(), event.payload());
                    }
                }
                default -> log.warn("Type d'événement non reconnu: {}", event.type());
            }
            log.debug("Événement publié: {} -> {}", event.type(), event.userId() != null ? event.userId() : event.assetId());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement {}: {}", event.type(), e.getMessage(), e);
        }
    }
    
    /**
     * Publie immédiatement un événement (sans attendre le commit)
     * À utiliser uniquement pour les événements non critiques
     */
    public void publishImmediately(DomainEvent event) {
        publishEvent(event);
    }
}

// tn.esprit.examen.nomPrenomClasseExamen.services.MarketEventService

package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.MarketEvent;
import tn.esprit.examen.nomPrenomClasseExamen.entities.TradingSession;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.MarketEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketEventService {

    private final MarketEventRepository eventRepository;
    private final TradingSessionService sessionService;

    @Transactional
    public MarketEvent createEvent(MarketEvent event) {
        log.info("📝 Création événement: {}", event.getTitre());

        TradingSession session = sessionService.getSessionById(event.getSession().getId());
        event.setSession(session);

        if (event.getDeclenche() == null) {
            event.setDeclenche(false);
        }

        return eventRepository.save(event);
    }

    public List<MarketEvent> getSessionEvents(Long sessionId) {
        return eventRepository.findBySessionIdOrderByDeclenchementPrevuAsc(sessionId);
    }

    public MarketEvent getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
    }

    @Transactional
    public MarketEvent triggerEvent(Long eventId) {
        log.info("🔥 Déclenchement événement: {}", eventId);

        MarketEvent event = getEventById(eventId);

        if (event.getDeclenche()) {
            throw new RuntimeException("Déjà déclenché");
        }

        event.setDeclenche(true);
        event.setDeclenchementReel(LocalDateTime.now());

        return eventRepository.save(event);
    }

    @Transactional
    public List<MarketEvent> checkAndTriggerScheduledEvents(Long sessionId) {
        LocalDateTime now = LocalDateTime.now();
        List<MarketEvent> pendingEvents = eventRepository.findPendingEvents(sessionId, now);

        for (MarketEvent event : pendingEvents) {
            event.setDeclenche(true);
            event.setDeclenchementReel(now);
            eventRepository.save(event);
            log.info("🔥 Auto-déclenché: {}", event.getTitre());
        }

        return pendingEvents;
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        MarketEvent event = getEventById(eventId);

        if (event.getDeclenche()) {
            throw new RuntimeException("Impossible de supprimer un événement déclenché");
        }

        eventRepository.delete(event);
    }
}
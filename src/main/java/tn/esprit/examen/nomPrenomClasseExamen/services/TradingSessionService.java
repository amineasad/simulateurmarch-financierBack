// tn.esprit.examen.nomPrenomClasseExamen.services.TradingSessionService

package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.TradingSession;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.TradingSessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingSessionService {

    private final TradingSessionRepository sessionRepository;

    @Transactional
    public TradingSession createSession(TradingSession session) {
        log.info("📝 Création session: {}", session.getNom());

        session.setCodeAcces(generateSessionCode());
        session.setStatus(SessionStatus.WAITING);

        if (session.getHeureFin() == null && session.getDureeMinutes() != null) {
            session.setHeureFin(session.getHeureDebut().plusMinutes(session.getDureeMinutes()));
        }

        return sessionRepository.save(session);
    }

    public List<TradingSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    public TradingSession getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session non trouvée: " + id));
    }

    public TradingSession getSessionByCode(String code) {
        return sessionRepository.findByCodeAcces(code)
                .orElseThrow(() -> new RuntimeException("Session non trouvée: " + code));
    }

    public List<TradingSession> getActiveSessions() {
        List<SessionStatus> activeStatuses = List.of(SessionStatus.WAITING, SessionStatus.OPEN);
        return sessionRepository.findByStatusIn(activeStatuses);
    }

    public List<TradingSession> getSessionsByCreator(Long creatorId) {
        return sessionRepository.findByCreateurIdOrderByCreatedAtDesc(creatorId);
    }

    @Transactional
    public TradingSession startSession(Long sessionId) {
        log.info("🚀 Démarrage session: {}", sessionId);
        TradingSession session = getSessionById(sessionId);

        if (session.getStatus() != SessionStatus.WAITING) {
            throw new RuntimeException("Session ne peut être démarrée");
        }

        session.setStatus(SessionStatus.OPEN);
        session.setHeureDebut(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    @Transactional
    public TradingSession pauseSession(Long sessionId) {
        TradingSession session = getSessionById(sessionId);
        session.setStatus(SessionStatus.PAUSED);
        return sessionRepository.save(session);
    }

    @Transactional
    public TradingSession resumeSession(Long sessionId) {
        TradingSession session = getSessionById(sessionId);
        session.setStatus(SessionStatus.OPEN);
        return sessionRepository.save(session);
    }

    @Transactional
    public TradingSession closeSession(Long sessionId) {
        log.info("🏁 Fermeture session: {}", sessionId);
        TradingSession session = getSessionById(sessionId);
        session.setStatus(SessionStatus.CLOSED);
        session.setHeureFin(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public boolean isSessionFull(Long sessionId) {
        Long count = sessionRepository.countParticipants(sessionId);
        TradingSession session = getSessionById(sessionId);
        return count >= session.getMaxParticipants();
    }

    public Long countParticipants(Long sessionId) {
        return sessionRepository.countParticipants(sessionId);
    }

    @Transactional
    public TradingSession updateSession(Long sessionId, TradingSession updatedSession) {
        TradingSession session = getSessionById(sessionId);
        session.setNom(updatedSession.getNom());
        session.setDescription(updatedSession.getDescription());
        session.setDureeMinutes(updatedSession.getDureeMinutes());
        session.setMaxParticipants(updatedSession.getMaxParticipants());
        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    private String generateSessionCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder("GAME-");

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        if (sessionRepository.findByCodeAcces(code.toString()).isPresent()) {
            return generateSessionCode();
        }

        return code.toString();
    }
}
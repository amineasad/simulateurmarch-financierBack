// tn.esprit.examen.nomPrenomClasseExamen.services.SessionParticipationService

package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionParticipation;
import tn.esprit.examen.nomPrenomClasseExamen.entities.SessionStatus;
import tn.esprit.examen.nomPrenomClasseExamen.entities.TradingSession;
import tn.esprit.examen.nomPrenomClasseExamen.entities.User;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.SessionParticipationRepository;
import tn.esprit.examen.nomPrenomClasseExamen.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionParticipationService {

    private final SessionParticipationRepository participationRepository;
    private final TradingSessionService sessionService;
    private final UserRepository userRepository;

    @Transactional
    public SessionParticipation joinSession(Long sessionId, Long userId) {
        log.info("👤 User {} rejoint session {}", userId, sessionId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        TradingSession session = sessionService.getSessionById(sessionId);

        if (participationRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new RuntimeException("Vous participez déjà");
        }

        if (sessionService.isSessionFull(sessionId)) {
            throw new RuntimeException("Session complète");
        }

        if (session.getStatus() != SessionStatus.WAITING && session.getStatus() != SessionStatus.OPEN) {
            throw new RuntimeException("Session n'accepte plus de participants");
        }

        SessionParticipation participation = SessionParticipation.builder()
                .session(session)
                .user(user)
                .cashActuel(session.getCashInitial())
                .valeurPortefeuille(session.getCashInitial())
                .rendement(0.0)
                .connecte(false)
                .nombreOrdres(0)
                .volumeTotal(0.0)
                .build();

        return participationRepository.save(participation);
    }

    @Transactional
    public SessionParticipation joinSessionByCode(String codeAcces, Long userId) {
        TradingSession session = sessionService.getSessionByCode(codeAcces);
        return joinSession(session.getId(), userId);
    }

    @Transactional
    public SessionParticipation connect(Long sessionId, Long userId) {
        SessionParticipation participation = participationRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Participation non trouvée"));

        participation.setConnecte(true);
        participation.setHeureConnexion(LocalDateTime.now());
        return participationRepository.save(participation);
    }

    @Transactional
    public SessionParticipation disconnect(Long sessionId, Long userId) {
        SessionParticipation participation = participationRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Participation non trouvée"));

        participation.setConnecte(false);
        participation.setHeureDeconnexion(LocalDateTime.now());
        return participationRepository.save(participation);
    }

    public SessionParticipation getParticipation(Long sessionId, Long userId) {
        return participationRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Participation non trouvée"));
    }

    public List<SessionParticipation> getParticipants(Long sessionId) {
        return participationRepository.findBySessionIdOrderByClassementAsc(sessionId);
    }

    public List<SessionParticipation> getLeaderboard(Long sessionId) {
        return participationRepository.getLeaderboard(sessionId);
    }

    public Long countActiveParticipants(Long sessionId) {
        return participationRepository.countActiveParticipants(sessionId);
    }

    @Transactional
    public SessionParticipation updatePortfolio(Long sessionId, Long userId, Double cashActuel, Double valeurPortefeuille) {
        SessionParticipation participation = getParticipation(sessionId, userId);
        participation.setCashActuel(cashActuel);
        participation.setValeurPortefeuille(valeurPortefeuille);

        TradingSession session = sessionService.getSessionById(sessionId);
        double rendement = ((valeurPortefeuille - session.getCashInitial()) / session.getCashInitial()) * 100;
        participation.setRendement(rendement);

        return participationRepository.save(participation);
    }

    @Transactional
    public void incrementOrderCount(Long sessionId, Long userId, Double orderValue) {
        SessionParticipation participation = getParticipation(sessionId, userId);
        participation.setNombreOrdres(participation.getNombreOrdres() + 1);
        participation.setVolumeTotal(participation.getVolumeTotal() + orderValue);
        participationRepository.save(participation);
    }
}
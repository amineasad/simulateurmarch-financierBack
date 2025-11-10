// src/main/java/tn/esprit/examen/nomPrenomClasseExamen/services/TradingSessionService.java
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

    /**
     * ✅ VERSION CORRIGÉE - Crée une session avec des dates cohérentes
     */
    @Transactional
    public TradingSession createSession(TradingSession session) {
        log.info("📝 Création session: {}", session.getNom());

        // Générer un code unique
        session.setCodeAcces(generateSessionCode());

        // Statut initial
        if (session.getStatus() == null) {
            session.setStatus(SessionStatus.WAITING);
        }

        // ✅ FIX CRITIQUE: Gérer correctement les dates
        LocalDateTime now = LocalDateTime.now();

        // Si heureDebut n'est pas définie ou est dans le passé, la mettre dans le futur
        if (session.getHeureDebut() == null || session.getHeureDebut().isBefore(now)) {
            // Démarrer dans 1 minute
            session.setHeureDebut(now.plusMinutes(1));
            log.info("⏰ HeureDebut ajustée au futur: {}", session.getHeureDebut());
        }

        // ✅ IMPORTANT: Calculer heureFin = heureDebut + durée
        if (session.getDureeMinutes() != null && session.getDureeMinutes() > 0) {
            session.setHeureFin(session.getHeureDebut().plusMinutes(session.getDureeMinutes()));
            log.info("⏰ HeureFin calculée: {} (début + {} min)",
                    session.getHeureFin(), session.getDureeMinutes());
        } else {
            throw new IllegalArgumentException("La durée doit être positive");
        }

        // ✅ VALIDATION: S'assurer que fin > début
        if (session.getHeureFin().isBefore(session.getHeureDebut()) ||
                session.getHeureFin().equals(session.getHeureDebut())) {
            throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début");
        }

        // Valeurs par défaut
        if (session.getMaxParticipants() == null) {
            session.setMaxParticipants(50);
        }

        if (session.getCashInitial() == null) {
            session.setCashInitial(100000.0);
        }

        if (session.getModeAccelere() == null) {
            session.setModeAccelere(false);
        }

        // Timestamps
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(LocalDateTime.now());
        }
        session.setUpdatedAt(LocalDateTime.now());

        TradingSession savedSession = sessionRepository.save(session);

        // Logs de débogage
        log.info("✅ Session créée: id={}, code={}", savedSession.getId(), savedSession.getCodeAcces());
        log.info("   📅 Début: {}", savedSession.getHeureDebut());
        log.info("   📅 Fin:   {}", savedSession.getHeureFin());
        log.info("   ⏱️  Durée: {} minutes", savedSession.getDureeMinutes());

        return savedSession;
    }

    public List<TradingSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    public TradingSession getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session non trouvée: " + id));
    }

    public TradingSession getSessionByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code d'accès ne peut pas être vide");
        }
        return sessionRepository.findByCodeAcces(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Session non trouvée avec le code: " + code));
    }

    public List<TradingSession> getActiveSessions() {
        List<SessionStatus> activeStatuses = List.of(
                SessionStatus.WAITING,
                SessionStatus.OPEN,
                SessionStatus.PAUSED
        );
        return sessionRepository.findByStatusIn(activeStatuses);
    }

    public List<TradingSession> getSessionsByCreator(Long creatorId) {
        return sessionRepository.findByCreateurIdOrderByCreatedAtDesc(creatorId);
    }

    /**
     * ✅ CORRIGÉ: Démarre une session
     */
    @Transactional
    public TradingSession startSession(Long sessionId) {
        log.info("🚀 Démarrage session: {}", sessionId);
        TradingSession session = getSessionById(sessionId);

        if (session.getStatus() != SessionStatus.WAITING) {
            throw new IllegalStateException(
                    "La session ne peut être démarrée. Statut actuel: " + session.getStatus()
            );
        }

        session.setStatus(SessionStatus.OPEN);

        // ✅ Mettre à jour heureDebut au moment du démarrage
        LocalDateTime now = LocalDateTime.now();
        session.setHeureDebut(now);

        // ✅ Recalculer heureFin basée sur la durée
        if (session.getDureeMinutes() != null) {
            session.setHeureFin(now.plusMinutes(session.getDureeMinutes()));
        }

        session.setUpdatedAt(now);

        TradingSession updated = sessionRepository.save(session);
        log.info("✅ Session démarrée: début={}, fin={}",
                updated.getHeureDebut(), updated.getHeureFin());

        return updated;
    }

    @Transactional
    public TradingSession pauseSession(Long sessionId) {
        log.info("⏸️ Mise en pause session: {}", sessionId);
        TradingSession session = getSessionById(sessionId);

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new IllegalStateException("Seules les sessions ouvertes peuvent être mises en pause");
        }

        session.setStatus(SessionStatus.PAUSED);
        session.setUpdatedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    @Transactional
    public TradingSession resumeSession(Long sessionId) {
        log.info("▶️ Reprise session: {}", sessionId);
        TradingSession session = getSessionById(sessionId);

        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new IllegalStateException("Seules les sessions en pause peuvent être reprises");
        }

        session.setStatus(SessionStatus.OPEN);
        session.setUpdatedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    @Transactional
    public TradingSession closeSession(Long sessionId) {
        log.info("🏁 Fermeture session: {}", sessionId);
        TradingSession session = getSessionById(sessionId);

        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new IllegalStateException("La session est déjà fermée");
        }

        session.setStatus(SessionStatus.CLOSED);
        session.setHeureFin(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

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

        if (session.getStatus() == SessionStatus.OPEN || session.getStatus() == SessionStatus.CLOSED) {
            throw new IllegalStateException(
                    "Impossible de modifier une session ouverte ou fermée"
            );
        }

        session.setNom(updatedSession.getNom());
        session.setDescription(updatedSession.getDescription());
        session.setDureeMinutes(updatedSession.getDureeMinutes());
        session.setMaxParticipants(updatedSession.getMaxParticipants());
        session.setUpdatedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        TradingSession session = getSessionById(sessionId);

        if (session.getStatus() == SessionStatus.OPEN) {
            throw new IllegalStateException(
                    "Impossible de supprimer une session en cours"
            );
        }

        log.info("🗑️ Suppression session: {}", sessionId);
        sessionRepository.deleteById(sessionId);
    }

    /**
     * Génère un code unique pour la session
     */
    private String generateSessionCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            StringBuilder code = new StringBuilder("GAME-");

            for (int i = 0; i < 6; i++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }

            String generatedCode = code.toString();

            if (sessionRepository.findByCodeAcces(generatedCode).isEmpty()) {
                return generatedCode;
            }
        }

        throw new RuntimeException("Impossible de générer un code unique après " + maxAttempts + " tentatives");
    }

    /**
     * ✅ BONUS: Méthode de debug pour vérifier les dates
     */
    public void debugSession(Long sessionId) {
        TradingSession session = getSessionById(sessionId);
        LocalDateTime now = LocalDateTime.now();

        log.info("🔍 DEBUG Session #{}", sessionId);
        log.info("   Nom: {}", session.getNom());
        log.info("   Status: {}", session.getStatus());
        log.info("   Maintenant:   {}", now);
        log.info("   HeureDebut:   {}", session.getHeureDebut());
        log.info("   HeureFin:     {}", session.getHeureFin());
        log.info("   Durée config: {} min", session.getDureeMinutes());

        if (session.getHeureDebut() != null && session.getHeureFin() != null) {
            long dureeReelle = java.time.Duration.between(
                    session.getHeureDebut(),
                    session.getHeureFin()
            ).toMinutes();

            log.info("   Durée réelle: {} min", dureeReelle);
            log.info("   Début < Maintenant: {}", session.getHeureDebut().isBefore(now));
            log.info("   Maintenant < Fin: {}", now.isBefore(session.getHeureFin()));
            log.info("   Début < Fin: {}", session.getHeureDebut().isBefore(session.getHeureFin()));

            if (session.getHeureDebut().isBefore(now) && now.isBefore(session.getHeureFin())) {
                long minutesEcoulees = java.time.Duration.between(session.getHeureDebut(), now).toMinutes();
                long minutesRestantes = java.time.Duration.between(now, session.getHeureFin()).toMinutes();
                log.info("   ✅ Session EN COURS: {}min écoulées, {}min restantes",
                        minutesEcoulees, minutesRestantes);
            } else if (now.isBefore(session.getHeureDebut())) {
                long minutesAvantDebut = java.time.Duration.between(now, session.getHeureDebut()).toMinutes();
                log.info("   ⏳ Session PAS ENCORE DÉMARRÉE: dans {}min", minutesAvantDebut);
            } else {
                log.info("   🏁 Session TERMINÉE");
            }
        }
    }
}
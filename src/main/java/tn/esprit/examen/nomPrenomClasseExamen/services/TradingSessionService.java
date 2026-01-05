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
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingSessionService {

    private final TradingSessionRepository sessionRepository;

    /**
     * ✅ VERSION FINALE - Crée une session avec time scaling
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

        LocalDateTime now = LocalDateTime.now();

        // ✅ NOUVEAU : Gérer le time scaling
        // Si durationMinutesReal n'est pas défini, utiliser dureeMinutes ou défaut 60
        if (session.getDurationMinutesReal() == null) {
            if (session.getDureeMinutes() != null) {
                session.setDurationMinutesReal(session.getDureeMinutes());
            } else {
                session.setDurationMinutesReal(60); // 1 heure par défaut
            }
        }

        // Si timeScaleFactor n'est pas défini, utiliser modeAccelere ou défaut
        if (session.getTimeScaleFactor() == null) {
            if (session.getModeAccelere() != null && session.getModeAccelere()) {
                session.setTimeScaleFactor(6.5); // Mode accéléré = 6.5x
            } else {
                session.setTimeScaleFactor(1.0); // Temps réel = 1x
            }
        }

        // Synchroniser dureeMinutes avec durationMinutesReal (compatibilité)
        if (session.getDureeMinutes() == null) {
            session.setDureeMinutes(session.getDurationMinutesReal());
        }

        // Si heureDebut n'est pas définie ou est dans le passé, la mettre dans le futur
        if (session.getHeureDebut() == null || session.getHeureDebut().isBefore(now)) {
            session.setHeureDebut(now.plusMinutes(1));
            log.info("⏰ HeureDebut ajustée au futur: {}", session.getHeureDebut());
        }

        // ✅ IMPORTANT: Calculer heureFin = heureDebut + durée RÉELLE
        if (session.getDurationMinutesReal() > 0) {
            session.setHeureFin(session.getHeureDebut().plusMinutes(session.getDurationMinutesReal()));
        } else {
            throw new IllegalArgumentException("La durée réelle doit être positive");
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
            session.setModeAccelere(session.getTimeScaleFactor() > 1.0);
        }

        // Timestamps
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(LocalDateTime.now());
        }
        session.setUpdatedAt(LocalDateTime.now());

        TradingSession savedSession = sessionRepository.save(session);

        // ✅ LOGS ENRICHIS avec time scaling
        log.info("✅ Session créée: id={}, code={}", savedSession.getId(), savedSession.getCodeAcces());
        log.info("   📅 TEMPS RÉEL:");
        log.info("      - Début: {}", savedSession.getHeureDebut());
        log.info("      - Fin:   {}", savedSession.getHeureFin());
        log.info("      - Durée: {} minutes", savedSession.getDurationMinutesReal());
        log.info("   🎯 TIME SCALING:");
        log.info("      - Facteur: {}x", savedSession.getTimeScaleFactor());
        log.info("      - Durée virtuelle: {} minutes ({} heures de marché)",
                (long)(savedSession.getDurationMinutesReal() * savedSession.getTimeScaleFactor()),
                (long)(savedSession.getDurationMinutesReal() * savedSession.getTimeScaleFactor()) / 60);
        log.info("      - Description: {}", savedSession.getTimeScaleDescription());

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
     * ✅ VERSION FINALE - Démarre une session avec time scaling
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

        // ✅ Recalculer heureFin basée sur la durée RÉELLE
        if (session.getDurationMinutesReal() != null) {
            session.setHeureFin(now.plusMinutes(session.getDurationMinutesReal()));
        } else if (session.getDureeMinutes() != null) {
            session.setHeureFin(now.plusMinutes(session.getDureeMinutes()));
        }

        session.setUpdatedAt(now);

        TradingSession updated = sessionRepository.save(session);

        // ✅ LOGS ENRICHIS avec contexte virtuel
        LocalTime virtualTime = updated.getCurrentVirtualMarketTime();
        TradingSession.TradingPhase phase = updated.getCurrentTradingPhase();

        log.info("✅ Session démarrée:");
        log.info("   ⏰ TEMPS RÉEL: début={}, fin={}", updated.getHeureDebut(), updated.getHeureFin());
        log.info("   🎯 TEMPS VIRTUEL: marché ouvre à {} (phase: {})", virtualTime, phase.getFullDescription());
        log.info("   ⚡ Accélération: {}x", updated.getTimeScaleFactor());

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

        // ✅ NOUVEAU : Logger l'heure virtuelle de la pause
        LocalTime virtualTime = session.getCurrentVirtualMarketTime();
        TradingSession.TradingPhase phase = session.getCurrentTradingPhase();
        log.info("   🎯 Pause à l'heure virtuelle: {} ({})", virtualTime, phase.getDescription());

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

        // ✅ NOUVEAU : Logger l'heure virtuelle de la reprise
        LocalTime virtualTime = session.getCurrentVirtualMarketTime();
        TradingSession.TradingPhase phase = session.getCurrentTradingPhase();
        log.info("   🎯 Reprise à l'heure virtuelle: {} ({})", virtualTime, phase.getDescription());

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

        // ✅ NOUVEAU : Logger l'heure virtuelle de fermeture
        LocalTime virtualTime = session.getCurrentVirtualMarketTime();
        boolean marketWasOpen = session.isVirtualMarketOpen();
        log.info("   🎯 Fermeture à l'heure virtuelle: {} (marché était {})",
                virtualTime,
                marketWasOpen ? "ouvert" : "fermé");

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

        // ✅ NOUVEAU : Permettre de modifier les paramètres time scaling
        if (updatedSession.getDurationMinutesReal() != null) {
            session.setDurationMinutesReal(updatedSession.getDurationMinutesReal());
            // Synchroniser dureeMinutes
            session.setDureeMinutes(updatedSession.getDurationMinutesReal());
        } else if (updatedSession.getDureeMinutes() != null) {
            session.setDureeMinutes(updatedSession.getDureeMinutes());
            session.setDurationMinutesReal(updatedSession.getDureeMinutes());
        }

        if (updatedSession.getTimeScaleFactor() != null) {
            session.setTimeScaleFactor(updatedSession.getTimeScaleFactor());
            // Synchroniser modeAccelere
            session.setModeAccelere(updatedSession.getTimeScaleFactor() > 1.0);
        }

        if (updatedSession.getMaxParticipants() != null) {
            session.setMaxParticipants(updatedSession.getMaxParticipants());
        }

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
     * ✅ VERSION FINALE - Debug avec time scaling
     */
    public void debugSession(Long sessionId) {
        TradingSession session = getSessionById(sessionId);
        LocalDateTime now = LocalDateTime.now();

        log.info("🔍 DEBUG Session #{}", sessionId);
        log.info("   Nom: {}", session.getNom());
        log.info("   Status: {}", session.getStatus());

        log.info("   ⏰ TEMPS RÉEL:");
        log.info("      - Maintenant:   {}", now);
        log.info("      - HeureDebut:   {}", session.getHeureDebut());
        log.info("      - HeureFin:     {}", session.getHeureFin());
        log.info("      - Durée config: {} min", session.getDurationMinutesReal());

        if (session.getHeureDebut() != null && session.getHeureFin() != null) {
            long dureeReelle = java.time.Duration.between(
                    session.getHeureDebut(),
                    session.getHeureFin()
            ).toMinutes();

            log.info("      - Durée réelle: {} min", dureeReelle);
            log.info("      - Début < Maintenant: {}", session.getHeureDebut().isBefore(now));
            log.info("      - Maintenant < Fin: {}", now.isBefore(session.getHeureFin()));
            log.info("      - Début < Fin: {}", session.getHeureDebut().isBefore(session.getHeureFin()));

            // ✅ NOUVEAU : Afficher le temps VIRTUEL
            LocalTime virtualTime = session.getCurrentVirtualMarketTime();
            TradingSession.TradingPhase phase = session.getCurrentTradingPhase();
            boolean isMarketOpen = session.isVirtualMarketOpen();
            double progress = session.getProgressPercentage();

            log.info("   🎯 TEMPS VIRTUEL (Time Scaling):");
            log.info("      - Heure marché: {}", virtualTime);
            log.info("      - Phase trading: {}", phase.getFullDescription());
            log.info("      - Marché ouvert: {}", isMarketOpen);
            log.info("      - Progression: {:.1f}%", progress);
            log.info("      - Facteur accélération: {}x", session.getTimeScaleFactor());
            log.info("      - Description: {}", session.getTimeScaleDescription());

            // Calcul des temps écoulés/restants
            if (session.getHeureDebut().isBefore(now) && now.isBefore(session.getHeureFin())) {
                long minutesReellesEcoulees = java.time.Duration.between(session.getHeureDebut(), now).toMinutes();
                long minutesReellesRestantes = java.time.Duration.between(now, session.getHeureFin()).toMinutes();

                long minutesVirtuellesEcoulees = (long)(minutesReellesEcoulees * session.getTimeScaleFactor());
                long minutesVirtuellesRestantes = session.getVirtualMinutesRemaining();

                log.info("   ✅ Session EN COURS:");
                log.info("      - Temps réel: {}min écoulées, {}min restantes",
                        minutesReellesEcoulees, minutesReellesRestantes);
                log.info("      - Temps virtuel: {}min écoulées, {}min restantes jusqu'à clôture (16h)",
                        minutesVirtuellesEcoulees, minutesVirtuellesRestantes);

                // Afficher dans combien de temps réel le marché fermera virtuellement
                if (minutesVirtuellesRestantes > 0) {
                    long realMinutesUntilVirtualClose = (long)(minutesVirtuellesRestantes / session.getTimeScaleFactor());
                    log.info("      - Le marché virtuel fermera dans {}min réelles", realMinutesUntilVirtualClose);
                }

            } else if (now.isBefore(session.getHeureDebut())) {
                long minutesAvantDebut = java.time.Duration.between(now, session.getHeureDebut()).toMinutes();
                log.info("   ⏳ Session PAS ENCORE DÉMARRÉE: dans {}min", minutesAvantDebut);
            } else {
                log.info("   🏁 Session TERMINÉE");
                log.info("      - Marché virtuel fermé à: {}", virtualTime);
            }
        }
    }

    /**
     * ✅ NOUVEAU : Vérifie et ferme automatiquement les sessions expirées
     * À appeler régulièrement via @Scheduled
     */
    @Transactional
    public void checkAndCloseExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<TradingSession> openSessions = sessionRepository.findByStatus(SessionStatus.OPEN);

        for (TradingSession session : openSessions) {
            boolean shouldClose = false;
            String reason = "";

            // Vérifier si l'heure de fin réelle est dépassée
            if (session.getHeureFin() != null && now.isAfter(session.getHeureFin())) {
                shouldClose = true;
                reason = "durée réelle écoulée";
            }

            // ✅ NOUVEAU : Vérifier si le marché virtuel est fermé (16h00)
            if (!shouldClose && session.isVirtualMarketClosed()) {
                LocalTime virtualTime = session.getCurrentVirtualMarketTime();
                shouldClose = true;
                reason = String.format("marché virtuel fermé à %s", virtualTime);
            }

            if (shouldClose) {
                log.info("⏰ Auto-fermeture de la session #{} ({})", session.getId(), reason);
                try {
                    closeSession(session.getId());
                } catch (Exception e) {
                    log.error("❌ Erreur lors de l'auto-fermeture de la session #{}", session.getId(), e);
                }
            }
        }
    }

    /**
     * ✅ NOUVEAU : Récupère toutes les sessions qui nécessitent une vérification d'événements
     */
    public List<TradingSession> getSessionsForEventChecking() {
        return sessionRepository.findByStatus(SessionStatus.OPEN);
    }
}
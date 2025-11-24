package tn.esprit.examen.nomPrenomClasseExamen.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketReplayEngine {

    private final SimpMessagingTemplate messagingTemplate;
    private final TimeCompressionCalculator compressionCalculator;

    // ========== DONNÉES HISTORIQUES ==========

    @Data
    public static class MarketTick {
        private LocalDateTime timestamp;
        private String symbol;
        private double open;
        private double high;
        private double low;
        private double close;
        private long volume;
    }

    @Data
    public static class TickBatch {
        private LocalDateTime timestamp;
        private List<MarketTick> ticks;
    }

    @Data
    public static class ReplaySession {
        private Long sessionId;
        private TimeCompressionCalculator.CompressionConfig config;
        private List<TickBatch> tickBatches;

        // État de lecture
        private AtomicLong realStartTimeMs = new AtomicLong(0);
        private AtomicLong pausedAtMs = new AtomicLong(0);
        private AtomicLong totalPausedMs = new AtomicLong(0);
        private AtomicBoolean isPlaying = new AtomicBoolean(false);
        private AtomicBoolean isStopped = new AtomicBoolean(false);

        private int currentBatchIndex = 0;

        // Thread de diffusion
        private ScheduledExecutorService scheduler;
        private ScheduledFuture<?> broadcastTask;
    }

    private final Map<Long, ReplaySession> activeSessions = new ConcurrentHashMap<>();

    public void startReplaySession(
            Long sessionId,
            List<MarketTick> historicalData,
            TimeCompressionCalculator.CompressionConfig config) {

        log.info("🚀 Démarrage replay session {} avec {} ticks", sessionId, historicalData.size());

        List<TickBatch> batches = groupTicksByTimestamp(historicalData);
        log.info("📦 {} batches créés", batches.size());

        ReplaySession session = new ReplaySession();
        session.setSessionId(sessionId);
        session.setConfig(config);
        session.setTickBatches(batches);
        session.setScheduler(Executors.newScheduledThreadPool(1));

        activeSessions.put(sessionId, session);

        play(sessionId);
    }

    private List<TickBatch> groupTicksByTimestamp(List<MarketTick> ticks) {
        List<MarketTick> sortedTicks = ticks.stream()
                .sorted(Comparator.comparing(MarketTick::getTimestamp))
                .collect(Collectors.toList());

        Map<LocalDateTime, List<MarketTick>> grouped = sortedTicks.stream()
                .collect(Collectors.groupingBy(MarketTick::getTimestamp));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    TickBatch batch = new TickBatch();
                    batch.setTimestamp(entry.getKey());
                    batch.setTicks(entry.getValue());
                    return batch;
                })
                .collect(Collectors.toList());
    }

    public void play(Long sessionId) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) {
            log.error("❌ Session {} introuvable", sessionId);
            return;
        }

        if (session.isPlaying.get()) {
            log.warn("⚠️ Session {} déjà en cours de lecture", sessionId);
            return;
        }

        log.info("▶️ Lecture session {}", sessionId);

        long now = System.currentTimeMillis();

        if (session.getRealStartTimeMs().get() == 0) {
            session.getRealStartTimeMs().set(now);
        } else {
            long pauseDuration = now - session.getPausedAtMs().get();
            session.getTotalPausedMs().addAndGet(pauseDuration);
        }

        session.isPlaying.set(true);
        startBroadcastLoop(session);
    }

    public void pause(Long sessionId) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) return;

        log.info("⏸️ Pause session {}", sessionId);

        session.isPlaying.set(false);
        session.getPausedAtMs().set(System.currentTimeMillis());

        if (session.getBroadcastTask() != null) {
            session.getBroadcastTask().cancel(false);
        }
    }

    public void stop(Long sessionId) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) return;

        log.info("⏹️ Arrêt session {}", sessionId);

        session.isPlaying.set(false);
        session.isStopped.set(true);

        if (session.getBroadcastTask() != null) {
            session.getBroadcastTask().cancel(false);
        }

        if (session.getScheduler() != null) {
            session.getScheduler().shutdown();
        }

        activeSessions.remove(sessionId);
    }

    public void rewind(Long sessionId, int minutes) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) return;

        log.info("⏪ Rewind session {} de {} minutes", sessionId, minutes);

        boolean wasPlaying = session.isPlaying.get();
        if (wasPlaying) {
            pause(sessionId);
        }

        session.setCurrentBatchIndex(Math.max(0, session.getCurrentBatchIndex() - minutes));

        long msToSubtract = (long) (minutes * session.getConfig().getMsPerTick());
        session.getRealStartTimeMs().addAndGet(msToSubtract);

        if (wasPlaying) {
            play(sessionId);
        }
    }

    public void forward(Long sessionId, int minutes) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) return;

        log.info("⏩ Forward session {} de {} minutes", sessionId, minutes);

        boolean wasPlaying = session.isPlaying.get();
        if (wasPlaying) {
            pause(sessionId);
        }

        int maxIndex = session.getTickBatches().size() - 1;
        session.setCurrentBatchIndex(Math.min(maxIndex, session.getCurrentBatchIndex() + minutes));

        long msToSubtract = (long) (minutes * session.getConfig().getMsPerTick());
        session.getRealStartTimeMs().addAndGet(-msToSubtract);

        if (wasPlaying) {
            play(sessionId);
        }
    }

    public void seekToDate(Long sessionId, LocalDateTime targetDate) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) return;

        log.info("🎯 Seek session {} vers {}", sessionId, targetDate);

        boolean wasPlaying = session.isPlaying.get();
        if (wasPlaying) {
            pause(sessionId);
        }

        List<TickBatch> batches = session.getTickBatches();
        int targetIndex = 0;

        for (int i = 0; i < batches.size(); i++) {
            if (!batches.get(i).getTimestamp().isBefore(targetDate)) {
                targetIndex = i;
                break;
            }
        }

        session.setCurrentBatchIndex(targetIndex);

        long elapsedMs = (long) (targetIndex * session.getConfig().getMsPerTick());
        session.getRealStartTimeMs().set(System.currentTimeMillis() - elapsedMs);
        session.getTotalPausedMs().set(0);

        if (wasPlaying) {
            play(sessionId);
        }
    }

    /**
     * ✨ CORRECTION : Changement de vitesse avec support des RALENTIS
     */
    public void setSpeed(Long sessionId, double speedMultiplier) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) {
            log.error("❌ Session {} introuvable", sessionId);
            return;
        }

        // ✅ Validation renforcée
        if (speedMultiplier <= 0 || speedMultiplier > 10000) {
            log.warn("⚠️ Vitesse invalide: {}. Doit être entre 0.01 et 10000", speedMultiplier);
            return;
        }

        // ✅ Log détaillé selon le type de vitesse
        if (speedMultiplier < 1.0) {
            log.info("🐌 RALENTI activé session {} → ×{} ({}% vitesse normale)",
                    sessionId, speedMultiplier, (speedMultiplier * 100));
        } else if (speedMultiplier == 1.0) {
            log.info("▶️ Vitesse NORMALE session {} → ×1", sessionId);
        } else {
            log.info("⚡ ACCÉLÉRATION session {} → ×{}", sessionId, speedMultiplier);
        }

        boolean wasPlaying = session.isPlaying.get();

        // ✅ IMPORTANT : Arrêter la lecture actuelle
        if (wasPlaying) {
            pause(sessionId);
        }

        // ✅ Mettre à jour la vitesse dans la config
        session.getConfig().setSpeedMultiplier(speedMultiplier);

        // ✅ IMPORTANT : Redémarrer avec la nouvelle vitesse
        if (wasPlaying) {
            // Petit délai pour s'assurer que la pause est effective
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            play(sessionId);
        }
    }

    /**
     * ✨ CORRECTION : Boucle de diffusion avec calcul précis pour les RALENTIS
     */
    private void startBroadcastLoop(ReplaySession session) {
        TimeCompressionCalculator.CompressionConfig config = session.getConfig();

        // ✅ FORMULE CORRIGÉE pour supporter les ralentis
        // Pour ralenti ×0.1 : msPerTick / 0.1 = msPerTick × 10
        // Pour normal ×1 : msPerTick / 1 = msPerTick
        // Pour accéléré ×10 : msPerTick / 10
        double effectiveMsPerTick = config.getMsPerTick() / config.getSpeedMultiplier();

        // ✅ Pour les ralentis extrêmes (×0.1), on peut avoir des intervalles très longs
        long intervalMs = Math.max(1, Math.round(effectiveMsPerTick));

        // ✅ Log détaillé pour debug
        log.info("🔄 Démarrage boucle broadcast:");
        log.info("   - msPerTick config: {}", config.getMsPerTick());
        log.info("   - speedMultiplier: ×{}", config.getSpeedMultiplier());
        log.info("   - effectiveMsPerTick: {}", effectiveMsPerTick);
        log.info("   - intervalMs final: {} ms", intervalMs);

        if (config.getSpeedMultiplier() < 1.0) {
            log.info("   ⏱️ Mode RALENTI: 1 tick toutes les {} ms", intervalMs);
        } else if (config.getSpeedMultiplier() > 1.0) {
            log.info("   ⚡ Mode ACCÉLÉRÉ: 1 tick toutes les {} ms", intervalMs);
        }

        // ✅ Annuler l'ancienne tâche si elle existe
        if (session.getBroadcastTask() != null && !session.getBroadcastTask().isCancelled()) {
            session.getBroadcastTask().cancel(false);
        }

        // ✅ Créer la nouvelle tâche avec le bon intervalle
        session.setBroadcastTask(
                session.getScheduler().scheduleAtFixedRate(
                        () -> broadcastNextBatch(session),
                        0,
                        intervalMs,
                        TimeUnit.MILLISECONDS
                )
        );
    }

    private void broadcastNextBatch(ReplaySession session) {
        if (!session.isPlaying.get() || session.isStopped.get()) {
            return;
        }

        try {
            List<TickBatch> batches = session.getTickBatches();
            int currentIndex = session.getCurrentBatchIndex();

            if (currentIndex >= batches.size()) {
                log.info("🏁 Fin du replay session {}", session.getSessionId());
                stop(session.getSessionId());
                return;
            }

            TickBatch batch = batches.get(currentIndex);

            broadcastBatch(session.getSessionId(), batch);

            session.setCurrentBatchIndex(currentIndex + 1);

            // Stats toutes les 100 batches OU toutes les 10 batches en ralenti
            boolean shouldLogStats = currentIndex % 100 == 0
                    || (session.getConfig().getSpeedMultiplier() < 1.0 && currentIndex % 10 == 0);

            if (shouldLogStats) {
                broadcastProgress(session);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de la diffusion du batch", e);
        }
    }

    private void broadcastBatch(Long sessionId, TickBatch batch) {
        for (MarketTick tick : batch.getTicks()) {
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId + "/market-data",
                    tick
            );
        }

        log.debug("📡 Batch diffusé: {} @ {} ({} symboles)",
                sessionId, batch.getTimestamp(), batch.getTicks().size());
    }

    private void broadcastProgress(ReplaySession session) {
        long realElapsedMs = System.currentTimeMillis()
                - session.getRealStartTimeMs().get()
                - session.getTotalPausedMs().get();

        LocalDateTime virtualTime = compressionCalculator.calculateVirtualTime(
                session.getConfig(),
                realElapsedMs
        );

        double progress = compressionCalculator.calculateProgress(
                session.getConfig(),
                realElapsedMs
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("virtualTime", virtualTime.toString());
        stats.put("progress", progress);
        stats.put("currentTick", session.getCurrentBatchIndex());
        stats.put("totalTicks", session.getTickBatches().size());
        stats.put("isPlaying", session.isPlaying.get());
        stats.put("speedMultiplier", session.getConfig().getSpeedMultiplier());

        messagingTemplate.convertAndSend(
                "/topic/session/" + session.getSessionId() + "/replay-stats",
                stats
        );
    }

    public Map<String, Object> getSessionState(Long sessionId) {
        ReplaySession session = activeSessions.get(sessionId);
        if (session == null) {
            return Collections.emptyMap();
        }

        long realElapsedMs = System.currentTimeMillis()
                - session.getRealStartTimeMs().get()
                - session.getTotalPausedMs().get();

        LocalDateTime virtualTime = compressionCalculator.calculateVirtualTime(
                session.getConfig(),
                realElapsedMs
        );

        Map<String, Object> state = new HashMap<>();
        state.put("virtualTime", virtualTime);
        state.put("isPlaying", session.isPlaying.get());
        state.put("currentTick", session.getCurrentBatchIndex());
        state.put("totalTicks", session.getTickBatches().size());
        state.put("speedMultiplier", session.getConfig().getSpeedMultiplier());

        return state;
    }
}
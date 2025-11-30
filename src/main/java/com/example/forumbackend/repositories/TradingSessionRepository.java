package com.example.forumbackend.repositories;

import com.example.forumbackend.entities.SessionStatus;
import com.example.forumbackend.entities.TradingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradingSessionRepository extends JpaRepository<TradingSession, Long> {

    Optional<TradingSession> findByCodeAcces(String codeAcces);

    List<TradingSession> findByStatus(SessionStatus status);

    List<TradingSession> findByStatusIn(List<SessionStatus> statuses);

    List<TradingSession> findByCreateurIdOrderByCreatedAtDesc(Long createurId);

    @Query("SELECT COUNT(p) FROM SessionParticipation p WHERE p.session.id = :sessionId")
    Long countParticipants(@Param("sessionId") Long sessionId);
}

package tn.esprit.examen.nomPrenomClasseExamen.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "user_market_control", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "asset_id"}))
public class UserMarketControl {

    public enum State { OPEN, HALTED_USER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state = State.OPEN;

    @Column
    private String reason;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}


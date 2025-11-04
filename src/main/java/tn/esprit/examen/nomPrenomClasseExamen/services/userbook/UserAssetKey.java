package tn.esprit.examen.nomPrenomClasseExamen.services.userbook;

import java.util.Objects;

public final class UserAssetKey {
    private final Long userId;
    private final Long assetId;

    public UserAssetKey(Long userId, Long assetId) {
        this.userId = userId;
        this.assetId = assetId;
    }

    public Long getUserId() { return userId; }
    public Long getAssetId() { return assetId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAssetKey that = (UserAssetKey) o;
        return Objects.equals(userId, that.userId) && Objects.equals(assetId, that.assetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, assetId);
    }
}


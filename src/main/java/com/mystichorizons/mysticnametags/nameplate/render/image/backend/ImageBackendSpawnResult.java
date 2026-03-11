package com.mystichorizons.mysticnametags.nameplate.render.image.backend;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ImageBackendSpawnResult {

    private final boolean success;
    @Nullable
    private final UUID entityUuid;
    @Nullable
    private final String failureReason;

    private ImageBackendSpawnResult(boolean success,
                                    @Nullable UUID entityUuid,
                                    @Nullable String failureReason) {
        this.success = success;
        this.entityUuid = entityUuid;
        this.failureReason = failureReason;
    }

    public static ImageBackendSpawnResult success(UUID entityUuid) {
        return new ImageBackendSpawnResult(true, entityUuid, null);
    }

    public static ImageBackendSpawnResult failure(String reason) {
        return new ImageBackendSpawnResult(false, null, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public UUID getEntityUuid() {
        return entityUuid;
    }

    @Nullable
    public String getFailureReason() {
        return failureReason;
    }
}
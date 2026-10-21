package com.alejandrolunatech.disputes.application.port;

import com.alejandrolunatech.disputes.domain.Dispute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputeRepository {

    /** Stores a new dispute under its idempotency key. Throws {@link KeyTakenException} if the key is used. */
    Dispute create(Dispute dispute, String idempotencyKey, String requestFingerprint);

    /** Stores changes. Throws {@link ConcurrentUpdateException} when someone else changed it first. */
    Dispute update(Dispute dispute);

    Optional<Dispute> findById(UUID id);

    Optional<StoredRequest> findByIdempotencyKey(String idempotencyKey);

    List<Dispute> findNeedingTriage(int limit);

    /** A dispute plus the fingerprint of the request that created it, to compare a retried request. */
    record StoredRequest(Dispute dispute, String requestFingerprint) {
    }

    final class KeyTakenException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public KeyTakenException(String key) {
            super("idempotency key already used: " + key);
        }
    }

    final class ConcurrentUpdateException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ConcurrentUpdateException(UUID id) {
            super("dispute " + id + " was changed by someone else; reload and try again");
        }
    }
}

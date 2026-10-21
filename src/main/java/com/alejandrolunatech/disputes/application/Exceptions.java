package com.alejandrolunatech.disputes.application;

import java.util.UUID;

/** The expected failures of the use cases. The web layer maps each one to a problem response. */
public final class Exceptions {

    private Exceptions() {
    }

    public static final class DisputeNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public DisputeNotFoundException(UUID id) {
            super("no dispute with id " + id);
        }
    }

    public static final class TransactionNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public TransactionNotFoundException(String reference) {
            super("no transaction with reference " + reference);
        }
    }

    /** The policy refuses the dispute, for example because the amount is too high. */
    public static final class DisputeRefusedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public DisputeRefusedException(String reason) {
            super(reason);
        }
    }

    /** The same idempotency key was sent with a different request. */
    public static final class IdempotencyConflictException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public IdempotencyConflictException(String key) {
            super("idempotency key " + key + " was already used for a different request");
        }
    }
}

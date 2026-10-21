package com.alejandrolunatech.disputes.application.port;

import java.math.BigDecimal;

/**
 * The port to the language model. It gets masked text only, and what it returns is a proposal
 * that the application still checks.
 */
public interface TriageModel {

    Proposal propose(Request request);

    /** What the model sees: masked customer text plus transaction facts it may use. */
    record Request(String maskedStatement, String merchant, BigDecimal transactionAmount, String currency) {
    }

    /**
     * What the model proposes. {@code reasonCode} is a plain string on purpose: the model may
     * answer with anything, and turning it into our enum is the application's job.
     */
    record Proposal(String reasonCode, double confidence, BigDecimal mentionedAmount, Usage usage) {
    }

    record Usage(String model, int inputTokens, int outputTokens, long latencyMillis) {
    }

    /** The model could not answer: timeout, outage, rate limit or an unreadable reply. */
    final class UnavailableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.alejandrolunatech.disputes.application.port;

import java.time.Instant;

/** Every model call is recorded: who answered, what it cost in tokens, and how it ended. */
public interface ModelCallLog {

    void record(Entry entry);

    enum Outcome { ACCEPTED, SENT_TO_REVIEW, FAILED, SKIPPED_CIRCUIT_OPEN }

    record Entry(Instant at, String model, int inputTokens, int outputTokens, long latencyMillis, Outcome outcome,
                 String detail) {
    }
}

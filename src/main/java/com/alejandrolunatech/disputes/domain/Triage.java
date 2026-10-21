package com.alejandrolunatech.disputes.domain;

/**
 * The outcome of triage. Only two shapes exist: a reason decided by a known source, or a
 * message for the person who must decide.
 */
public sealed interface Triage {

    enum Source { MODEL, RULES, REVIEWER }

    record Decided(ReasonCode reasonCode, double confidence, Source source) implements Triage {

        public Decided {
            if (confidence < 0 || confidence > 1) {
                throw new IllegalArgumentException("confidence must be between 0 and 1");
            }
        }
    }

    record NeedsReview(String why) implements Triage {
    }
}

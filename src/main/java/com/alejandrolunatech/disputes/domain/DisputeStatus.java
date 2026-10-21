package com.alejandrolunatech.disputes.domain;

import java.util.EnumSet;
import java.util.Set;

/** The lifecycle. Every allowed move is listed here and nowhere else. */
public enum DisputeStatus {
    /** The reason is not decided yet: a person must triage it. */
    NEEDS_TRIAGE,
    /** The reason is known and the response deadline runs. */
    OPEN,
    EVIDENCE_SUBMITTED,
    WON,
    LOST,
    WITHDRAWN;

    public boolean canMoveTo(DisputeStatus next) {
        return allowedNext().contains(next);
    }

    public Set<DisputeStatus> allowedNext() {
        return switch (this) {
            case NEEDS_TRIAGE -> EnumSet.of(OPEN, WITHDRAWN);
            case OPEN -> EnumSet.of(EVIDENCE_SUBMITTED, WITHDRAWN);
            case EVIDENCE_SUBMITTED -> EnumSet.of(WON, LOST);
            case WON, LOST, WITHDRAWN -> EnumSet.noneOf(DisputeStatus.class);
        };
    }
}

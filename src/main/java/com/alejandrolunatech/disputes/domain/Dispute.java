package com.alejandrolunatech.disputes.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The aggregate. All state changes go through its methods, which enforce the lifecycle in
 * {@link DisputeStatus}. It knows nothing about HTTP, JPA or models.
 */
public final class Dispute {

    private final UUID id;
    private final Transaction transaction;
    private final Money disputedAmount;
    private final String maskedStatement;
    private final Instant openedAt;
    private DisputeStatus status;
    private Triage triage;
    private Instant responseDeadline;
    private final long version;

    private Dispute(UUID id, Transaction transaction, Money disputedAmount, String maskedStatement, Instant openedAt,
                    DisputeStatus status, Triage triage, Instant responseDeadline, long version) {
        this.id = Objects.requireNonNull(id);
        this.transaction = Objects.requireNonNull(transaction);
        this.disputedAmount = Objects.requireNonNull(disputedAmount);
        this.maskedStatement = Objects.requireNonNull(maskedStatement);
        this.openedAt = Objects.requireNonNull(openedAt);
        this.status = Objects.requireNonNull(status);
        this.triage = Objects.requireNonNull(triage);
        this.responseDeadline = responseDeadline;
        this.version = version;
    }

    /** Opens a dispute. A decided triage opens it straight away; otherwise it waits for a person. */
    public static Dispute open(UUID id, Transaction transaction, Money disputedAmount, String maskedStatement,
                               Triage triage, Instant now) {
        return switch (triage) {
            case Triage.Decided decided -> new Dispute(id, transaction, disputedAmount, maskedStatement, now,
                    DisputeStatus.OPEN, decided, DisputePolicy.responseDeadline(decided.reasonCode(), now), 0);
            case Triage.NeedsReview review -> new Dispute(id, transaction, disputedAmount, maskedStatement, now,
                    DisputeStatus.NEEDS_TRIAGE, review, null, 0);
        };
    }

    /** Rebuilds a dispute from storage. Not for business code. */
    public static Dispute restore(UUID id, Transaction transaction, Money disputedAmount, String maskedStatement,
                                  Instant openedAt, DisputeStatus status, Triage triage, Instant responseDeadline,
                                  long version) {
        return new Dispute(id, transaction, disputedAmount, maskedStatement, openedAt, status, triage,
                responseDeadline, version);
    }

    /** A reviewer decides the reason for a dispute that was waiting for triage. */
    public void decideReason(ReasonCode reason, Instant now) {
        moveTo(DisputeStatus.OPEN);
        this.triage = new Triage.Decided(reason, 1.0, Triage.Source.REVIEWER);
        this.responseDeadline = DisputePolicy.responseDeadline(reason, now);
    }

    public void submitEvidence() {
        moveTo(DisputeStatus.EVIDENCE_SUBMITTED);
    }

    public void resolve(boolean won) {
        moveTo(won ? DisputeStatus.WON : DisputeStatus.LOST);
        this.responseDeadline = null;
    }

    public void withdraw() {
        moveTo(DisputeStatus.WITHDRAWN);
        this.responseDeadline = null;
    }

    private void moveTo(DisputeStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalTransitionException(status, next);
        }
        this.status = next;
    }

    public UUID id() {
        return id;
    }

    public Transaction transaction() {
        return transaction;
    }

    public Money disputedAmount() {
        return disputedAmount;
    }

    public String maskedStatement() {
        return maskedStatement;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public DisputeStatus status() {
        return status;
    }

    public Triage triage() {
        return triage;
    }

    public Optional<Instant> responseDeadline() {
        return Optional.ofNullable(responseDeadline);
    }

    public long version() {
        return version;
    }
}

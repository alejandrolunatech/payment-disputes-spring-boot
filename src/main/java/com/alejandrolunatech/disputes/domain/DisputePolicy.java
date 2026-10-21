package com.alejandrolunatech.disputes.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * The deterministic rules. The numbers are made up for this project and are not any card
 * scheme's real rules.
 */
public final class DisputePolicy {

    /** How old a transaction may be and still be disputed. */
    public static final int DISPUTE_WINDOW_DAYS = 120;

    private DisputePolicy() {
    }

    /** Returns why the dispute may not be opened, or empty when it may. */
    public static Optional<String> refusalFor(Transaction transaction, Money disputedAmount, LocalDate today) {
        if (!disputedAmount.currency().equals(transaction.amount().currency())) {
            return Optional.of("disputed amount must be in " + transaction.amount().currency());
        }
        if (disputedAmount.isZero()) {
            return Optional.of("disputed amount must be more than zero");
        }
        if (disputedAmount.isGreaterThan(transaction.amount())) {
            return Optional.of("disputed amount is more than the transaction amount");
        }
        if (transaction.bookedOn().isAfter(today)) {
            return Optional.of("transaction is booked in the future");
        }
        if (transaction.bookedOn().plusDays(DISPUTE_WINDOW_DAYS).isBefore(today)) {
            return Optional.of("transaction is older than " + DISPUTE_WINDOW_DAYS + " days");
        }
        return Optional.empty();
    }

    /** Fraud is answered faster than everything else. */
    public static Instant responseDeadline(ReasonCode reason, Instant decidedAt) {
        Duration window = switch (reason) {
            case FRAUD -> Duration.ofDays(10);
            case NOT_RECEIVED, NOT_AS_DESCRIBED, DUPLICATE_CHARGE, CANCELLED_SUBSCRIPTION -> Duration.ofDays(20);
        };
        return decidedAt.atOffset(ZoneOffset.UTC).plus(window).toInstant();
    }
}

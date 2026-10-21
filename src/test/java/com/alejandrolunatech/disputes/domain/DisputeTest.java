package com.alejandrolunatech.disputes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DisputeTest {

    static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    static final Transaction TX = new Transaction("TX-1", Money.eur("100.00"), "Shop", LocalDate.parse("2026-10-01"));

    static Dispute decided(ReasonCode reason) {
        return Dispute.open(UUID.randomUUID(), TX, Money.eur("100.00"), "text",
                new Triage.Decided(reason, 0.9, Triage.Source.MODEL), NOW);
    }

    @Test
    void aDecidedTriageOpensWithADeadline() {
        Dispute dispute = decided(ReasonCode.NOT_RECEIVED);

        assertThat(dispute.status()).isEqualTo(DisputeStatus.OPEN);
        assertThat(dispute.responseDeadline()).contains(NOW.plus(Duration.ofDays(20)));
    }

    @Test
    void fraudIsAnsweredFaster() {
        assertThat(decided(ReasonCode.FRAUD).responseDeadline()).contains(NOW.plus(Duration.ofDays(10)));
    }

    @Test
    void anUndecidedTriageWaitsForAPersonWithoutADeadline() {
        Dispute dispute = Dispute.open(UUID.randomUUID(), TX, Money.eur("5.00"), "text",
                new Triage.NeedsReview("unclear"), NOW);

        assertThat(dispute.status()).isEqualTo(DisputeStatus.NEEDS_TRIAGE);
        assertThat(dispute.responseDeadline()).isEmpty();

        dispute.decideReason(ReasonCode.FRAUD, NOW);

        assertThat(dispute.status()).isEqualTo(DisputeStatus.OPEN);
        assertThat(dispute.triage()).isEqualTo(new Triage.Decided(ReasonCode.FRAUD, 1.0, Triage.Source.REVIEWER));
    }

    @Test
    void followsTheLifecycleToTheEnd() {
        Dispute dispute = decided(ReasonCode.DUPLICATE_CHARGE);
        dispute.submitEvidence();
        dispute.resolve(false);

        assertThat(dispute.status()).isEqualTo(DisputeStatus.LOST);
        assertThat(dispute.responseDeadline()).isEmpty();
    }

    @Test
    void refusesToSkipAStep() {
        Dispute dispute = decided(ReasonCode.FRAUD);

        assertThatThrownBy(() -> dispute.resolve(true)).isInstanceOf(IllegalTransitionException.class)
                .hasMessage("a dispute in status OPEN cannot move to WON");
    }

    @ParameterizedTest
    @EnumSource(value = DisputeStatus.class, names = {"WON", "LOST", "WITHDRAWN"})
    void endStatesAreFinal(DisputeStatus status) {
        assertThat(status.allowedNext()).isEmpty();
    }
}

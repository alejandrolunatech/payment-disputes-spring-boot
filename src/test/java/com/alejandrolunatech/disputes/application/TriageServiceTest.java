package com.alejandrolunatech.disputes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.alejandrolunatech.disputes.application.port.ModelCallLog.Outcome;
import com.alejandrolunatech.disputes.application.port.TriageModel.Proposal;
import com.alejandrolunatech.disputes.application.port.TriageModel.Usage;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.ReasonCode;
import com.alejandrolunatech.disputes.domain.Transaction;
import com.alejandrolunatech.disputes.domain.Triage;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TriageServiceTest {

    static final Transaction TX = new Transaction("TX-1", Money.eur("40.00"), "Shop", LocalDate.parse("2026-10-01"));

    final Fakes.Model model = new Fakes.Model();
    final Fakes.CallLog log = new Fakes.CallLog();
    final CircuitBreaker breaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
            .slidingWindowSize(4).minimumNumberOfCalls(4).failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1)).build());
    final TriageService triage = new TriageService(model, log, breaker, 0.75,
            Clock.fixed(Instant.parse("2026-10-21T09:00:00Z"), ZoneOffset.UTC));

    @Test
    void acceptsAConfidentKnownReason() {
        model.answers("NOT_RECEIVED", 0.91);

        assertThat(triage.triage("never came", TX))
                .isEqualTo(new Triage.Decided(ReasonCode.NOT_RECEIVED, 0.91, Triage.Source.MODEL));
        assertThat(log.last()).isEqualTo(Outcome.ACCEPTED);
        assertThat(log.entries.getLast().inputTokens()).isEqualTo(10);
    }

    @Test
    void sendsALowConfidenceToAPerson() {
        model.answers("FRAUD", 0.6);

        assertThat(triage.triage("hmm", TX)).isEqualTo(new Triage.NeedsReview("model confidence 0.60 is below 0.75"));
        assertThat(log.last()).isEqualTo(Outcome.SENT_TO_REVIEW);
    }

    @Test
    void sendsAnInventedReasonToAPerson() {
        model.answers("CUSTOMER_IS_ANGRY", 0.99);

        assertThat(triage.triage("hmm", TX)).isInstanceOf(Triage.NeedsReview.class);
    }

    @Test
    void sendsAnImpossibleConfidenceToAPerson() {
        model.answers("FRAUD", 1.7).answers("FRAUD", Double.NaN);

        assertThat(triage.triage("a", TX)).isEqualTo(new Triage.NeedsReview("model gave an invalid confidence"));
        assertThat(triage.triage("b", TX)).isEqualTo(new Triage.NeedsReview("model gave an invalid confidence"));
    }

    @Test
    void doesNotTrustAStatementThatClaimsMoreThanWasPaid() {
        model.answers(new Proposal("FRAUD", 0.95, new BigDecimal("400.00"), new Usage("fake", 1, 1, 1)));

        assertThat(triage.triage("they took 400 euro", TX))
                .isEqualTo(new Triage.NeedsReview("the statement mentions more than the transaction amount"));
    }

    @Test
    void fallsBackToRulesWhenTheModelIsDown() {
        model.fails();

        assertThat(triage.triage("I was charged twice", TX))
                .isEqualTo(new Triage.Decided(ReasonCode.DUPLICATE_CHARGE, 1.0, Triage.Source.RULES));
        assertThat(log.last()).isEqualTo(Outcome.FAILED);
    }

    @Test
    void sendsToAPersonWhenTheModelIsDownAndNoRuleMatches() {
        model.fails();

        assertThat(triage.triage("something odd", TX))
                .isEqualTo(new Triage.NeedsReview("model unavailable; no rule matched"));
    }

    @Test
    void stopsCallingAFailingModel() {
        model.fails().fails().fails().fails();
        for (int i = 0; i < 4; i++) {
            triage.triage("charged twice", TX);
        }
        int callsBefore = model.requests.size();

        Triage result = triage.triage("charged twice", TX);

        assertThat(model.requests).hasSize(callsBefore);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(log.last()).isEqualTo(Outcome.SKIPPED_CIRCUIT_OPEN);
        assertThat(result).isEqualTo(new Triage.Decided(ReasonCode.DUPLICATE_CHARGE, 1.0, Triage.Source.RULES));
    }

    @Test
    void sendsTheModelOnlyWhatItNeeds() {
        triage.triage("masked text", TX);

        assertThat(model.requests.getLast().maskedStatement()).isEqualTo("masked text");
        assertThat(model.requests.getLast().merchant()).isEqualTo("Shop");
        assertThat(model.requests.getLast().transactionAmount()).isEqualByComparingTo("40.00");
    }
}

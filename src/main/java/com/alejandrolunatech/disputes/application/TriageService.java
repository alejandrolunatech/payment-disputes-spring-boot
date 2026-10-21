package com.alejandrolunatech.disputes.application;

import com.alejandrolunatech.disputes.application.port.ModelCallLog;
import com.alejandrolunatech.disputes.application.port.ModelCallLog.Outcome;
import com.alejandrolunatech.disputes.application.port.TriageModel;
import com.alejandrolunatech.disputes.application.port.TriageModel.Proposal;
import com.alejandrolunatech.disputes.domain.ReasonCode;
import com.alejandrolunatech.disputes.domain.Transaction;
import com.alejandrolunatech.disputes.domain.Triage;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Deterministic core, probabilistic edge. The model proposes a reason; this class decides
 * whether to accept it, send it to a person, or fall back to rules when the model is down.
 */
public class TriageService {

    private final TriageModel model;
    private final ModelCallLog callLog;
    private final CircuitBreaker breaker;
    private final double minConfidence;
    private final Clock clock;

    public TriageService(TriageModel model, ModelCallLog callLog, CircuitBreaker breaker, double minConfidence,
                         Clock clock) {
        this.model = Objects.requireNonNull(model);
        this.callLog = Objects.requireNonNull(callLog);
        this.breaker = Objects.requireNonNull(breaker);
        this.minConfidence = minConfidence;
        this.clock = Objects.requireNonNull(clock);
    }

    /** {@code maskedStatement} must already be masked: this method sends it out of the system. */
    public Triage triage(String maskedStatement, Transaction transaction) {
        if (!breaker.tryAcquirePermission()) {
            log(null, Outcome.SKIPPED_CIRCUIT_OPEN, "model calls paused");
            return fallback(maskedStatement, "model paused after repeated failures");
        }
        long started = System.nanoTime();
        Proposal proposal;
        try {
            proposal = model.propose(new TriageModel.Request(maskedStatement, transaction.merchant(),
                    transaction.amount().amount(), transaction.amount().currency()));
            breaker.onSuccess(System.nanoTime() - started, TimeUnit.NANOSECONDS);
        } catch (TriageModel.UnavailableException e) {
            breaker.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, e);
            log(null, Outcome.FAILED, e.getMessage());
            return fallback(maskedStatement, "model unavailable");
        }
        Triage triage = check(proposal, transaction);
        log(proposal.usage(), triage instanceof Triage.Decided ? Outcome.ACCEPTED : Outcome.SENT_TO_REVIEW,
                triage instanceof Triage.NeedsReview review ? review.why() : proposal.reasonCode());
        return triage;
    }

    /** Every check here is plain code. The first one that fails sends the dispute to a person. */
    Triage check(Proposal proposal, Transaction transaction) {
        Optional<ReasonCode> reason = Arrays.stream(ReasonCode.values())
                .filter(code -> code.name().equals(proposal.reasonCode()))
                .findFirst();
        if (reason.isEmpty()) {
            return new Triage.NeedsReview("model proposed an unknown reason: " + proposal.reasonCode());
        }
        if (Double.isNaN(proposal.confidence()) || proposal.confidence() < 0 || proposal.confidence() > 1) {
            return new Triage.NeedsReview("model gave an invalid confidence");
        }
        if (proposal.confidence() < minConfidence) {
            return new Triage.NeedsReview("model confidence %.2f is below %.2f".formatted(proposal.confidence(), minConfidence));
        }
        if (proposal.mentionedAmount() != null
                && proposal.mentionedAmount().compareTo(transaction.amount().amount()) > 0) {
            return new Triage.NeedsReview("the statement mentions more than the transaction amount");
        }
        return new Triage.Decided(reason.get(), proposal.confidence(), Triage.Source.MODEL);
    }

    private Triage fallback(String maskedStatement, String why) {
        return KeywordRules.match(maskedStatement)
                .<Triage>map(reason -> new Triage.Decided(reason, 1.0, Triage.Source.RULES))
                .orElseGet(() -> new Triage.NeedsReview(why + "; no rule matched"));
    }

    private void log(TriageModel.Usage usage, Outcome outcome, String detail) {
        callLog.record(new ModelCallLog.Entry(clock.instant(),
                usage == null ? "none" : usage.model(),
                usage == null ? 0 : usage.inputTokens(),
                usage == null ? 0 : usage.outputTokens(),
                usage == null ? 0 : usage.latencyMillis(),
                outcome, detail));
    }
}

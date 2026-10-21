package com.alejandrolunatech.disputes.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alejandrolunatech.disputes.TestcontainersConfiguration;
import com.alejandrolunatech.disputes.application.port.DisputeRepository;
import com.alejandrolunatech.disputes.application.port.TransactionCatalog;
import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.ReasonCode;
import com.alejandrolunatech.disputes.domain.Triage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** The adapter against the real schema: round trips, the unique key, and optimistic locking. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JpaDisputeRepositoryTest {

    @Autowired
    DisputeRepository disputes;

    @Autowired
    TransactionCatalog transactions;

    Dispute newDispute(Triage triage) {
        return Dispute.open(UUID.randomUUID(), transactions.findByReference("TX-1003").orElseThrow(),
                Money.eur("100.00"), "masked", triage, Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void storesAndRestoresBothTriageShapes() {
        Dispute decided = disputes.create(newDispute(new Triage.Decided(ReasonCode.FRAUD, 0.81, Triage.Source.MODEL)),
                "repo-key-1", "f".repeat(64));
        Dispute review = disputes.create(newDispute(new Triage.NeedsReview("model unavailable")), "repo-key-2",
                "f".repeat(64));

        assertThat(disputes.findById(decided.id()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(decided);
        assertThat(disputes.findById(review.id()).orElseThrow().triage())
                .isEqualTo(new Triage.NeedsReview("model unavailable"));
        assertThat(disputes.findByIdempotencyKey("repo-key-2").orElseThrow().requestFingerprint())
                .isEqualTo("f".repeat(64));
    }

    @Test
    void anIdempotencyKeyIsStoredOnce() {
        disputes.create(newDispute(new Triage.NeedsReview("x")), "repo-key-3", "a".repeat(64));

        assertThatThrownBy(() -> disputes.create(newDispute(new Triage.NeedsReview("x")), "repo-key-3", "a".repeat(64)))
                .isInstanceOf(DisputeRepository.KeyTakenException.class);
    }

    @Test
    void theSecondOfTwoConcurrentChangesLoses() {
        Dispute stored = disputes.create(newDispute(new Triage.Decided(ReasonCode.FRAUD, 0.9, Triage.Source.MODEL)),
                "repo-key-4", "b".repeat(64));
        Dispute first = disputes.findById(stored.id()).orElseThrow();
        Dispute second = disputes.findById(stored.id()).orElseThrow();

        first.submitEvidence();
        assertThat(disputes.update(first).version()).isEqualTo(stored.version() + 1);

        second.withdraw();
        assertThatThrownBy(() -> disputes.update(second)).isInstanceOf(DisputeRepository.ConcurrentUpdateException.class);
    }
}

package com.alejandrolunatech.disputes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The table shape. The domain {@code Dispute} never sees this class. */
@Entity
@Table(name = "disputes")
class DisputeEntity {

    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "transaction_reference", nullable = false, updatable = false)
    TransactionEntity transaction;

    @Column(name = "disputed_amount", nullable = false, updatable = false)
    BigDecimal disputedAmount;

    @Column(nullable = false, updatable = false)
    String currency;

    @Column(name = "masked_statement", nullable = false, updatable = false)
    String maskedStatement;

    @Column(nullable = false)
    String status;

    @Column(name = "reason_code")
    String reasonCode;

    @Column(name = "triage_source")
    String triageSource;

    @Column(name = "triage_confidence")
    BigDecimal triageConfidence;

    @Column(name = "review_note")
    String reviewNote;

    @Column(name = "opened_at", nullable = false, updatable = false)
    Instant openedAt;

    @Column(name = "response_deadline")
    Instant responseDeadline;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true)
    String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, updatable = false)
    String requestFingerprint;

    @Version
    Long version;

    protected DisputeEntity() {
    }
}

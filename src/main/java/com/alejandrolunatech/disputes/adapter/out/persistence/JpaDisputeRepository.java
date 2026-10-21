package com.alejandrolunatech.disputes.adapter.out.persistence;

import com.alejandrolunatech.disputes.application.port.DisputeRepository;
import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.DisputeStatus;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.ReasonCode;
import com.alejandrolunatech.disputes.domain.Triage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Maps between the domain aggregate and its table. The only class that knows both. */
@Repository
class JpaDisputeRepository implements DisputeRepository {

    private final DisputeJpaRepository disputes;
    private final TransactionJpaRepository transactions;

    JpaDisputeRepository(DisputeJpaRepository disputes, TransactionJpaRepository transactions) {
        this.disputes = disputes;
        this.transactions = transactions;
    }

    @Override
    @Transactional
    public Dispute create(Dispute dispute, String idempotencyKey, String requestFingerprint) {
        DisputeEntity entity = new DisputeEntity();
        entity.id = dispute.id();
        // A loaded entity, not getReferenceById: toDomain reads its fields directly, and a lazy proxy has none.
        entity.transaction = transactions.findById(dispute.transaction().reference())
                .orElseThrow(() -> new IllegalStateException("unknown transaction: " + dispute.transaction().reference()));
        entity.disputedAmount = dispute.disputedAmount().amount();
        entity.currency = dispute.disputedAmount().currency();
        entity.maskedStatement = dispute.maskedStatement();
        entity.openedAt = dispute.openedAt();
        entity.idempotencyKey = idempotencyKey;
        entity.requestFingerprint = requestFingerprint;
        copyChangeableFields(dispute, entity);
        try {
            return toDomain(disputes.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new KeyTakenException(idempotencyKey);
        }
    }

    @Override
    @Transactional
    public Dispute update(Dispute dispute) {
        DisputeEntity entity = disputes.findById(dispute.id())
                .orElseThrow(() -> new IllegalStateException("dispute vanished: " + dispute.id()));
        if (entity.version != dispute.version()) {
            throw new ConcurrentUpdateException(dispute.id());
        }
        copyChangeableFields(dispute, entity);
        try {
            return toDomain(disputes.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentUpdateException(dispute.id());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dispute> findById(UUID id) {
        return disputes.findById(id).map(JpaDisputeRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredRequest> findByIdempotencyKey(String idempotencyKey) {
        return disputes.findByIdempotencyKey(idempotencyKey)
                .map(entity -> new StoredRequest(toDomain(entity), entity.requestFingerprint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> findNeedingTriage(int limit) {
        return disputes.findByStatusOrderByOpenedAtAsc(DisputeStatus.NEEDS_TRIAGE.name(), Limit.of(limit)).stream()
                .map(JpaDisputeRepository::toDomain)
                .toList();
    }

    private static void copyChangeableFields(Dispute dispute, DisputeEntity entity) {
        entity.status = dispute.status().name();
        entity.responseDeadline = dispute.responseDeadline().orElse(null);
        switch (dispute.triage()) {
            case Triage.Decided decided -> {
                entity.reasonCode = decided.reasonCode().name();
                entity.triageSource = decided.source().name();
                entity.triageConfidence = BigDecimal.valueOf(decided.confidence()).setScale(3, RoundingMode.HALF_UP);
                entity.reviewNote = null;
            }
            case Triage.NeedsReview review -> {
                entity.reasonCode = null;
                entity.triageSource = null;
                entity.triageConfidence = null;
                entity.reviewNote = review.why().length() > 300 ? review.why().substring(0, 300) : review.why();
            }
        }
    }

    private static Dispute toDomain(DisputeEntity entity) {
        Triage triage = entity.reasonCode == null
                ? new Triage.NeedsReview(entity.reviewNote == null ? "" : entity.reviewNote)
                : new Triage.Decided(ReasonCode.valueOf(entity.reasonCode), entity.triageConfidence.doubleValue(),
                        Triage.Source.valueOf(entity.triageSource));
        return Dispute.restore(entity.id, JpaTransactionCatalog.toDomain(entity.transaction),
                new Money(entity.disputedAmount, entity.currency), entity.maskedStatement, entity.openedAt,
                DisputeStatus.valueOf(entity.status), triage, entity.responseDeadline, entity.version);
    }
}

package com.alejandrolunatech.disputes.application;

import com.alejandrolunatech.disputes.application.Exceptions.DisputeNotFoundException;
import com.alejandrolunatech.disputes.application.Exceptions.DisputeRefusedException;
import com.alejandrolunatech.disputes.application.Exceptions.IdempotencyConflictException;
import com.alejandrolunatech.disputes.application.Exceptions.TransactionNotFoundException;
import com.alejandrolunatech.disputes.application.port.DisputeRepository;
import com.alejandrolunatech.disputes.application.port.DisputeRepository.StoredRequest;
import com.alejandrolunatech.disputes.application.port.TransactionCatalog;
import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.DisputePolicy;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.ReasonCode;
import com.alejandrolunatech.disputes.domain.Transaction;
import com.alejandrolunatech.disputes.domain.Triage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The use cases. No database transaction is held open while the model is called: the
 * dispute is built first and stored in one short write at the end.
 */
public class DisputeService {

    public record OpenDisputeCommand(String transactionReference, Money disputedAmount, String statement) {

        public OpenDisputeCommand {
            Objects.requireNonNull(transactionReference, "transactionReference");
            Objects.requireNonNull(disputedAmount, "disputedAmount");
            Objects.requireNonNull(statement, "statement");
        }

        /** Identifies the request, so a retry with the same key can be told apart from a different request. */
        String fingerprint() {
            String request = transactionReference + "\n" + disputedAmount.amount().toPlainString() + "\n"
                    + disputedAmount.currency() + "\n" + statement;
            try {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(request.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 is always available", e);
            }
        }
    }

    private final DisputeRepository disputes;
    private final TransactionCatalog transactions;
    private final TriageService triage;
    private final Clock clock;

    public DisputeService(DisputeRepository disputes, TransactionCatalog transactions, TriageService triage,
                          Clock clock) {
        this.disputes = Objects.requireNonNull(disputes);
        this.transactions = Objects.requireNonNull(transactions);
        this.triage = Objects.requireNonNull(triage);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Opens a dispute once per idempotency key. A retry of the same request returns the same dispute. */
    public Dispute open(OpenDisputeCommand command, String idempotencyKey) {
        var existing = disputes.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return sameRequestOrConflict(existing.get(), command, idempotencyKey);
        }
        Transaction transaction = transactions.findByReference(command.transactionReference())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionReference()));
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        DisputePolicy.refusalFor(transaction, command.disputedAmount(), today).ifPresent(reason -> {
            throw new DisputeRefusedException(reason);
        });
        String masked = PiiMasker.mask(command.statement());
        Triage decision = triage.triage(masked, transaction);
        Dispute dispute = Dispute.open(UUID.randomUUID(), transaction, command.disputedAmount(), masked, decision,
                clock.instant());
        try {
            return disputes.create(dispute, idempotencyKey, command.fingerprint());
        } catch (DisputeRepository.KeyTakenException raced) {
            StoredRequest winner = disputes.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> raced);
            return sameRequestOrConflict(winner, command, idempotencyKey);
        }
    }

    private static Dispute sameRequestOrConflict(StoredRequest stored, OpenDisputeCommand command, String key) {
        if (!stored.requestFingerprint().equals(command.fingerprint())) {
            throw new IdempotencyConflictException(key);
        }
        return stored.dispute();
    }

    public Dispute get(UUID id) {
        return disputes.findById(id).orElseThrow(() -> new DisputeNotFoundException(id));
    }

    public List<Dispute> reviewQueue(int limit) {
        return disputes.findNeedingTriage(limit);
    }

    public Dispute decideReason(UUID id, ReasonCode reason) {
        return change(id, dispute -> dispute.decideReason(reason, clock.instant()));
    }

    public Dispute submitEvidence(UUID id) {
        return change(id, Dispute::submitEvidence);
    }

    public Dispute resolve(UUID id, boolean won) {
        return change(id, dispute -> dispute.resolve(won));
    }

    public Dispute withdraw(UUID id) {
        return change(id, Dispute::withdraw);
    }

    private Dispute change(UUID id, Consumer<Dispute> action) {
        Dispute dispute = get(id);
        action.accept(dispute);
        return disputes.update(dispute);
    }
}

package com.alejandrolunatech.disputes.application;

import com.alejandrolunatech.disputes.application.port.DisputeRepository;
import com.alejandrolunatech.disputes.application.port.ModelCallLog;
import com.alejandrolunatech.disputes.application.port.TransactionCatalog;
import com.alejandrolunatech.disputes.application.port.TriageModel;
import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.DisputeStatus;
import com.alejandrolunatech.disputes.domain.Transaction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Hand-written test doubles for the ports. Each one is a few lines and shows the port's contract. */
final class Fakes {

    private Fakes() {
    }

    static final class Transactions implements TransactionCatalog {

        private final Map<String, Transaction> byReference = new HashMap<>();

        Transactions with(Transaction transaction) {
            byReference.put(transaction.reference(), transaction);
            return this;
        }

        @Override
        public Optional<Transaction> findByReference(String reference) {
            return Optional.ofNullable(byReference.get(reference));
        }
    }

    static final class Disputes implements DisputeRepository {

        final Map<UUID, Dispute> byId = new HashMap<>();
        final Map<String, StoredRequest> byKey = new HashMap<>();
        /** When set, the next create behaves as if another request stored this one first. */
        StoredRequest raceWinner;

        @Override
        public Dispute create(Dispute dispute, String idempotencyKey, String requestFingerprint) {
            if (raceWinner != null) {
                byKey.put(idempotencyKey, raceWinner);
                raceWinner = null;
            }
            if (byKey.containsKey(idempotencyKey)) {
                throw new KeyTakenException(idempotencyKey);
            }
            byId.put(dispute.id(), dispute);
            byKey.put(idempotencyKey, new StoredRequest(dispute, requestFingerprint));
            return dispute;
        }

        @Override
        public Dispute update(Dispute dispute) {
            byId.put(dispute.id(), dispute);
            return dispute;
        }

        @Override
        public Optional<Dispute> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<StoredRequest> findByIdempotencyKey(String idempotencyKey) {
            return Optional.ofNullable(byKey.get(idempotencyKey));
        }

        @Override
        public List<Dispute> findNeedingTriage(int limit) {
            return byId.values().stream().filter(d -> d.status() == DisputeStatus.NEEDS_TRIAGE).limit(limit).toList();
        }
    }

    static final class CallLog implements ModelCallLog {

        final List<Entry> entries = new ArrayList<>();

        @Override
        public void record(Entry entry) {
            entries.add(entry);
        }

        Outcome last() {
            return entries.getLast().outcome();
        }
    }

    /** Answers with queued replies; a queued exception is thrown instead. */
    static final class Model implements TriageModel {

        private final Deque<Function<Request, Proposal>> replies = new ArrayDeque<>();
        final List<Request> requests = new ArrayList<>();

        Model answers(String reason, double confidence) {
            replies.add(request -> new Proposal(reason, confidence, null, new Usage("fake", 10, 5, 3)));
            return this;
        }

        Model answers(Proposal proposal) {
            replies.add(request -> proposal);
            return this;
        }

        Model fails() {
            replies.add(request -> {
                throw new UnavailableException("down", null);
            });
            return this;
        }

        @Override
        public Proposal propose(Request request) {
            requests.add(request);
            return replies.isEmpty() ? new Proposal("FRAUD", 0.9, null, new Usage("fake", 10, 5, 3))
                    : replies.poll().apply(request);
        }
    }
}

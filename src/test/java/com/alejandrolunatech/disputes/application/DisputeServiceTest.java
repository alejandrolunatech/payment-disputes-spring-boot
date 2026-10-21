package com.alejandrolunatech.disputes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alejandrolunatech.disputes.application.DisputeService.OpenDisputeCommand;
import com.alejandrolunatech.disputes.application.Exceptions.DisputeRefusedException;
import com.alejandrolunatech.disputes.application.Exceptions.IdempotencyConflictException;
import com.alejandrolunatech.disputes.application.Exceptions.TransactionNotFoundException;
import com.alejandrolunatech.disputes.application.port.DisputeRepository.StoredRequest;
import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.DisputeStatus;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.Transaction;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DisputeServiceTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T09:00:00Z"), ZoneOffset.UTC);
    static final Transaction TX = new Transaction("TX-1", Money.eur("60.00"), "Shop", LocalDate.parse("2026-10-10"));

    final Fakes.Model model = new Fakes.Model();
    final Fakes.Disputes disputes = new Fakes.Disputes();
    final DisputeService service = new DisputeService(disputes, new Fakes.Transactions().with(TX),
            new TriageService(model, new Fakes.CallLog(), CircuitBreaker.ofDefaults("test"), 0.75, CLOCK), CLOCK);

    static OpenDisputeCommand command(String amount, String statement) {
        return new OpenDisputeCommand("TX-1", Money.eur(amount), statement);
    }

    @Test
    void masksPersonalDataBeforeTheModelSeesIt() {
        Dispute dispute = service.open(command("60.00", "Not me! Mail ana@example.com or call +31 6 1234 5678"),
                "key-1");

        assertThat(model.requests.getLast().maskedStatement()).isEqualTo("Not me! Mail [EMAIL] or call [PHONE]");
        assertThat(dispute.maskedStatement()).doesNotContain("ana@example.com");
    }

    @Test
    void aRetryDoesNotCallTheModelAgain() {
        Dispute first = service.open(command("60.00", "not me"), "key-1");
        Dispute second = service.open(command("60.00", "not me"), "key-1");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(model.requests).hasSize(1);
    }

    @Test
    void aReusedKeyWithAnotherRequestIsAConflict() {
        service.open(command("60.00", "not me"), "key-1");

        assertThatThrownBy(() -> service.open(command("59.00", "not me"), "key-1"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void losingARaceToTheSameKeyReturnsTheWinner() {
        Dispute winner = service.open(command("60.00", "not me"), "key-other");
        disputes.raceWinner = new StoredRequest(winner, command("60.00", "not me").fingerprint());

        assertThat(service.open(command("60.00", "not me"), "key-race").id()).isEqualTo(winner.id());
    }

    @Test
    void thePolicyIsCheckedBeforeTheModelIsPaid() {
        assertThatThrownBy(() -> service.open(command("60.01", "not me"), "key-1"))
                .isInstanceOf(DisputeRefusedException.class);
        assertThat(model.requests).isEmpty();
    }

    @Test
    void anUnknownTransactionIsNotFound() {
        assertThatThrownBy(() -> service.open(new OpenDisputeCommand("TX-404", Money.eur("1.00"), "x"), "key-1"))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void theFingerprintIsAStableHash() {
        assertThat(command("60.00", "not me").fingerprint())
                .hasSize(64)
                .isEqualTo(command("60", "not me").fingerprint())
                .isNotEqualTo(command("60.00", "not me!").fingerprint());
    }

    @Test
    void movesThroughTheLifecycle() {
        Dispute dispute = service.open(command("60.00", "not me"), "key-1");

        service.submitEvidence(dispute.id());

        assertThat(service.resolve(dispute.id(), true).status()).isEqualTo(DisputeStatus.WON);
    }
}

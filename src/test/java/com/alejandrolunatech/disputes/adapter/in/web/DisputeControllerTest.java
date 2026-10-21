package com.alejandrolunatech.disputes.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.alejandrolunatech.disputes.application.DisputeService;
import com.alejandrolunatech.disputes.application.Exceptions.DisputeNotFoundException;
import com.alejandrolunatech.disputes.application.port.DisputeRepository.ConcurrentUpdateException;
import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.ReasonCode;
import com.alejandrolunatech.disputes.domain.Transaction;
import com.alejandrolunatech.disputes.domain.Triage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The HTTP contract alone: status codes, problem types and validation, with the use cases mocked. */
@WebMvcTest(DisputeController.class)
class DisputeControllerTest {

    static final String BODY = """
            {"transactionReference":"TX-1","amount":10.00,"currency":"EUR","statement":"not me"}""";

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    DisputeService disputes;

    static Dispute dispute() {
        return Dispute.open(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                new Transaction("TX-1", Money.eur("10.00"), "Shop", LocalDate.parse("2026-10-01")), Money.eur("10.00"),
                "not me", new Triage.Decided(ReasonCode.FRAUD, 0.9, Triage.Source.MODEL),
                Instant.parse("2026-10-21T09:00:00Z"));
    }

    @Test
    void createdDisputesHaveALocationAndTheirNextSteps() {
        given(disputes.open(any(), eq("key-12345"))).willReturn(dispute());

        assertThat(mvc.post().uri("/api/disputes").header("Idempotency-Key", "key-12345")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .hasStatus(201)
                .hasHeader("Location", "/api/disputes/00000000-0000-0000-0000-000000000001")
                .bodyJson().isLenientlyEqualTo("""
                        {"status":"OPEN","allowedNext":["EVIDENCE_SUBMITTED","WITHDRAWN"],
                         "triage":{"reasonCode":"FRAUD","decidedBy":"MODEL","confidence":0.9},
                         "responseDeadline":"2026-10-31T09:00:00Z","version":0}""");
    }

    @Test
    void theIdempotencyKeyIsRequired() {
        assertThat(mvc.post().uri("/api/disputes").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void aMalformedIdempotencyKeyIsRefused() {
        assertThat(mvc.post().uri("/api/disputes").header("Idempotency-Key", "short")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void anInvalidBodyIsAProblem() {
        assertThat(mvc.post().uri("/api/disputes").header("Idempotency-Key", "key-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transactionReference\":\"\",\"amount\":10.001,\"currency\":\"euro\",\"statement\":\"x\"}"))
                .hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void anUnknownDisputeIsAProblemWithAStableType() {
        UUID id = UUID.randomUUID();
        given(disputes.get(id)).willThrow(new DisputeNotFoundException(id));

        assertThat(mvc.get().uri("/api/disputes/{id}", id))
                .hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.type").asString().endsWith("/docs/problems.md#not-found");
    }

    @Test
    void aLostUpdateIsAConflict() {
        UUID id = UUID.randomUUID();
        given(disputes.submitEvidence(id)).willThrow(new ConcurrentUpdateException(id));

        assertThat(mvc.post().uri("/api/disputes/{id}/evidence", id))
                .hasStatus(409).bodyJson().extractingPath("$.type").asString().endsWith("#concurrent-update");
    }

    @Test
    void anUnknownReasonCodeIsRefused() {
        assertThat(mvc.post().uri("/api/disputes/{id}/reason", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reasonCode\":\"BORED\"}"))
                .hasStatus(400);
    }

    @Test
    void theReviewQueueLimitIsBounded() {
        assertThat(mvc.get().uri("/api/disputes/review-queue?limit=1000")).hasStatus(400);
    }
}

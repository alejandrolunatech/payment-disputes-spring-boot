package com.alejandrolunatech.disputes.adapter.in.web;

import com.alejandrolunatech.disputes.adapter.in.web.DisputeApi.DecideReasonRequest;
import com.alejandrolunatech.disputes.adapter.in.web.DisputeApi.DisputeResponse;
import com.alejandrolunatech.disputes.adapter.in.web.DisputeApi.OpenDisputeRequest;
import com.alejandrolunatech.disputes.adapter.in.web.DisputeApi.ResolveRequest;
import com.alejandrolunatech.disputes.adapter.in.web.DisputeApi.ReviewQueueResponse;
import com.alejandrolunatech.disputes.application.DisputeService;
import com.alejandrolunatech.disputes.application.DisputeService.OpenDisputeCommand;
import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin by design: validate the shape, call one use case, map the result. No business rules here.
 * Constraints on parameters are checked by Spring MVC itself (no {@code @Validated}), so a
 * violation becomes the same 400 problem response as a bad body.
 */
@RestController
@RequestMapping("/api/disputes")
class DisputeController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final DisputeService disputes;

    DisputeController(DisputeService disputes) {
        this.disputes = disputes;
    }

    /** A retry with the same key and body returns the dispute created the first time. */
    @PostMapping
    ResponseEntity<DisputeResponse> open(
            @RequestHeader(IDEMPOTENCY_KEY) @Pattern(regexp = "[A-Za-z0-9_-]{8,100}") String idempotencyKey,
            @Valid @RequestBody OpenDisputeRequest request) {
        Dispute dispute = disputes.open(new OpenDisputeCommand(request.transactionReference(),
                new Money(request.amount(), request.currency()), request.statement()), idempotencyKey);
        return ResponseEntity.created(URI.create("/api/disputes/" + dispute.id())).body(DisputeResponse.of(dispute));
    }

    @GetMapping("/{id}")
    DisputeResponse get(@PathVariable UUID id) {
        return DisputeResponse.of(disputes.get(id));
    }

    /** Disputes a person must triage, oldest first. */
    @GetMapping("/review-queue")
    ReviewQueueResponse reviewQueue(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return new ReviewQueueResponse(disputes.reviewQueue(limit).stream().map(DisputeResponse::of).toList());
    }

    @PostMapping("/{id}/reason")
    DisputeResponse decideReason(@PathVariable UUID id, @Valid @RequestBody DecideReasonRequest request) {
        return DisputeResponse.of(disputes.decideReason(id, request.reasonCode()));
    }

    @PostMapping("/{id}/evidence")
    DisputeResponse submitEvidence(@PathVariable UUID id) {
        return DisputeResponse.of(disputes.submitEvidence(id));
    }

    @PostMapping("/{id}/resolution")
    DisputeResponse resolve(@PathVariable UUID id, @Valid @RequestBody ResolveRequest request) {
        return DisputeResponse.of(disputes.resolve(id, request.won()));
    }

    @PostMapping("/{id}/withdrawal")
    DisputeResponse withdraw(@PathVariable UUID id) {
        return DisputeResponse.of(disputes.withdraw(id));
    }
}

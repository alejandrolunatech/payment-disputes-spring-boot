package com.alejandrolunatech.disputes.adapter.in.web;

import com.alejandrolunatech.disputes.domain.Dispute;
import com.alejandrolunatech.disputes.domain.DisputeStatus;
import com.alejandrolunatech.disputes.domain.ReasonCode;
import com.alejandrolunatech.disputes.domain.Triage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** The JSON shapes of the API. The domain never leaves this package as-is. */
final class DisputeApi {

    private DisputeApi() {
    }

    record OpenDisputeRequest(
            @NotBlank @Size(max = 40) String transactionReference,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 9, fraction = 2) BigDecimal amount,
            @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 4000) String statement) {
    }

    record DecideReasonRequest(@NotNull ReasonCode reasonCode) {
    }

    record ResolveRequest(@NotNull Boolean won) {
    }

    /**
     * @param triage how the reason was decided, or why a person must decide it
     * @param allowedNext the statuses this dispute may move to now, so a client never has to
     *                    hard-code the lifecycle
     */
    record DisputeResponse(UUID id, String transactionReference, BigDecimal amount, String currency,
                           String statement, DisputeStatus status, TriageView triage, Instant responseDeadline,
                           Instant openedAt, Set<DisputeStatus> allowedNext, long version) {

        static DisputeResponse of(Dispute dispute) {
            return new DisputeResponse(dispute.id(), dispute.transaction().reference(),
                    dispute.disputedAmount().amount(), dispute.disputedAmount().currency(),
                    dispute.maskedStatement(), dispute.status(), TriageView.of(dispute.triage()),
                    dispute.responseDeadline().orElse(null), dispute.openedAt(), dispute.status().allowedNext(),
                    dispute.version());
        }
    }

    record TriageView(ReasonCode reasonCode, Triage.Source decidedBy, Double confidence, String needsReviewBecause) {

        static TriageView of(Triage triage) {
            return switch (triage) {
                case Triage.Decided decided -> new TriageView(decided.reasonCode(), decided.source(),
                        decided.confidence(), null);
                case Triage.NeedsReview review -> new TriageView(null, null, null, review.why());
            };
        }
    }

    record ReviewQueueResponse(List<DisputeResponse> disputes) {
    }
}

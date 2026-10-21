package com.alejandrolunatech.disputes.adapter.in.web;

import com.alejandrolunatech.disputes.application.Exceptions.DisputeNotFoundException;
import com.alejandrolunatech.disputes.application.Exceptions.DisputeRefusedException;
import com.alejandrolunatech.disputes.application.Exceptions.IdempotencyConflictException;
import com.alejandrolunatech.disputes.application.Exceptions.TransactionNotFoundException;
import com.alejandrolunatech.disputes.application.port.DisputeRepository.ConcurrentUpdateException;
import com.alejandrolunatech.disputes.domain.IllegalTransitionException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error is an RFC 9457 problem with a stable {@code type}, so clients can branch on it
 * without parsing messages. Framework errors (bad JSON, failed validation, missing header)
 * come from the base class in the same format.
 */
@RestControllerAdvice
class ProblemResponses extends ResponseEntityExceptionHandler {

    private static final String TYPES = "https://github.com/alejandrolunatech/payment-disputes-spring-boot/blob/main/docs/problems.md#";

    @ExceptionHandler({DisputeNotFoundException.class, TransactionNotFoundException.class})
    ProblemDetail notFound(RuntimeException e) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Not found", e);
    }

    @ExceptionHandler(DisputeRefusedException.class)
    ProblemDetail refused(DisputeRefusedException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "dispute-refused", "Dispute refused", e);
    }

    /** 422, as the IETF Idempotency-Key draft asks for a key reused with a different body. */
    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail keyReused(IdempotencyConflictException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "idempotency-key-reused", "Idempotency key reused", e);
    }

    @ExceptionHandler(IllegalTransitionException.class)
    ProblemDetail illegalTransition(IllegalTransitionException e) {
        return problem(HttpStatus.CONFLICT, "illegal-transition", "Not allowed in this status", e);
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    ProblemDetail concurrentUpdate(ConcurrentUpdateException e) {
        return problem(HttpStatus.CONFLICT, "concurrent-update", "Changed by someone else; read it again", e);
    }

    /** Thrown by value objects such as Money for input that passed the shape checks but is still invalid. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidValue(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-value", "Invalid value", e);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, RuntimeException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setType(URI.create(TYPES + type));
        problem.setTitle(title);
        return problem;
    }
}

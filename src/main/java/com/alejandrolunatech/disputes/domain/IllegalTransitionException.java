package com.alejandrolunatech.disputes.domain;

public final class IllegalTransitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IllegalTransitionException(DisputeStatus from, DisputeStatus to) {
        super("a dispute in status " + from + " cannot move to " + to);
    }
}

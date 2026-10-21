package com.alejandrolunatech.disputes.domain;

import java.time.LocalDate;
import java.util.Objects;

/** The card transaction being disputed, as the ledger knows it. Read-only for this service. */
public record Transaction(String reference, Money amount, String merchant, LocalDate bookedOn) {

    public Transaction {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(merchant, "merchant");
        Objects.requireNonNull(bookedOn, "bookedOn");
    }
}

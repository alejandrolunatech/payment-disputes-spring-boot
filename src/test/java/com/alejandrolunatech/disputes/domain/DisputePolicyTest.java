package com.alejandrolunatech.disputes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DisputePolicyTest {

    static final LocalDate TODAY = LocalDate.parse("2026-10-21");

    static Transaction bookedDaysAgo(int days) {
        return new Transaction("TX-1", Money.eur("50.00"), "Shop", TODAY.minusDays(days));
    }

    @Test
    void allowsAPartOfARecentTransaction() {
        assertThat(DisputePolicy.refusalFor(bookedDaysAgo(3), Money.eur("20.00"), TODAY)).isEmpty();
    }

    @Test
    void refusesMoreThanWasPaid() {
        assertThat(DisputePolicy.refusalFor(bookedDaysAgo(3), Money.eur("50.01"), TODAY))
                .contains("disputed amount is more than the transaction amount");
    }

    @Test
    void refusesAnotherCurrencyAndZero() {
        assertThat(DisputePolicy.refusalFor(bookedDaysAgo(3), new Money(BigDecimal.TEN, "USD"), TODAY))
                .contains("disputed amount must be in EUR");
        assertThat(DisputePolicy.refusalFor(bookedDaysAgo(3), Money.eur("0"), TODAY))
                .contains("disputed amount must be more than zero");
    }

    @Test
    void theWindowIncludesItsLastDay() {
        assertThat(DisputePolicy.refusalFor(bookedDaysAgo(120), Money.eur("1.00"), TODAY)).isEmpty();
        assertThat(DisputePolicy.refusalFor(bookedDaysAgo(121), Money.eur("1.00"), TODAY))
                .contains("transaction is older than 120 days");
    }

    @Test
    void refusesATransactionFromTheFuture() {
        assertThat(DisputePolicy.refusalFor(bookedDaysAgo(-1), Money.eur("1.00"), TODAY))
                .contains("transaction is booked in the future");
    }
}

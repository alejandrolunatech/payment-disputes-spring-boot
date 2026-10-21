package com.alejandrolunatech.disputes.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** An amount in one currency, with at most two decimals. Never a double. */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("amount has more than two decimals");
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must be an ISO 4217 code");
        }
        amount = amount.setScale(2);
    }

    public static Money eur(String amount) {
        return new Money(new BigDecimal(amount), "EUR");
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("cannot compare " + currency + " with " + other.currency);
        }
    }
}

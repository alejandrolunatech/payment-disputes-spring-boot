package com.alejandrolunatech.disputes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void keepsTwoDecimals() {
        assertThat(new Money(new BigDecimal("12.5"), "EUR").amount()).isEqualByComparingTo("12.50")
                .hasToString("12.50");
    }

    @Test
    void refusesWhatIsNotMoney() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.eur("-1.00"));
        assertThatIllegalArgumentException().isThrownBy(() -> Money.eur("1.005"));
        assertThatIllegalArgumentException().isThrownBy(() -> new Money(BigDecimal.ONE, "eur"));
    }

    @Test
    void comparesOnlyTheSameCurrency() {
        assertThat(Money.eur("10.00").isGreaterThan(Money.eur("9.99"))).isTrue();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.eur("10.00").isGreaterThan(new Money(BigDecimal.ONE, "USD")));
    }
}

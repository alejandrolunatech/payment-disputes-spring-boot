package com.alejandrolunatech.disputes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.alejandrolunatech.disputes.domain.ReasonCode;
import org.junit.jupiter.api.Test;

class KeywordRulesTest {

    @Test
    void matchesTheFirstRuleInOrder() {
        assertThat(KeywordRules.match("Not me, and I was charged twice")).contains(ReasonCode.FRAUD);
        assertThat(KeywordRules.match("The parcel NEVER ARRIVED")).contains(ReasonCode.NOT_RECEIVED);
    }

    @Test
    void saysNothingWhenItDoesNotKnow() {
        assertThat(KeywordRules.match("I want my money back")).isEmpty();
    }
}

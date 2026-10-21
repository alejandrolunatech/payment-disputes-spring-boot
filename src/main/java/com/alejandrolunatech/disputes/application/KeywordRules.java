package com.alejandrolunatech.disputes.application;

import com.alejandrolunatech.disputes.domain.ReasonCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The fallback when the model is unavailable. Worse than the model on messy text, but always
 * there and always the same. Rules are checked in order; the first match wins.
 */
public final class KeywordRules {

    private static final Map<ReasonCode, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put(ReasonCode.FRAUD, List.of("didn't make", "did not make", "not me", "stolen", "don't recognise",
                "don't recognize", "never authorised", "never authorized"));
        RULES.put(ReasonCode.DUPLICATE_CHARGE, List.of("charged twice", "double charge", "charged two times", "duplicate"));
        RULES.put(ReasonCode.CANCELLED_SUBSCRIPTION, List.of("cancelled my subscription", "canceled my subscription",
                "after cancelling", "after canceling", "unsubscribed"));
        RULES.put(ReasonCode.NOT_RECEIVED, List.of("never arrived", "never received", "not delivered", "didn't arrive"));
        RULES.put(ReasonCode.NOT_AS_DESCRIBED, List.of("broken", "damaged", "not as described", "wrong item", "fake"));
    }

    private KeywordRules() {
    }

    public static Optional<ReasonCode> match(String statement) {
        String text = statement.toLowerCase(Locale.ROOT);
        return RULES.entrySet().stream()
                .filter(rule -> rule.getValue().stream().anyMatch(text::contains))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}

package com.alejandrolunatech.disputes.adapter.out.llm;

import com.alejandrolunatech.disputes.application.port.TriageModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOT A LANGUAGE MODEL. A deterministic stand-in so the service runs offline, in tests and in
 * CI without a key or any cost. It counts cue phrases and turns the count into a confidence,
 * which is enough to exercise every path of the triage checks. Results from it say nothing
 * about how a real model performs.
 *
 * <p>Demo hook: a statement containing {@code #outage} makes it fail like a model outage, so
 * the circuit breaker and the rules fallback can be seen working locally.
 */
public class ScriptedTriageModel implements TriageModel {

    public static final String NAME = "scripted-stand-in";

    private static final Map<String, List<String>> CUES = Map.of(
            "FRAUD", List.of("did not make", "didn't make", "not me", "stolen", "never authorised", "never authorized",
                    "don't recognise", "don't recognize", "unknown charge"),
            "NOT_RECEIVED", List.of("never arrived", "not received", "never received", "didn't arrive", "no delivery",
                    "still waiting"),
            "NOT_AS_DESCRIBED", List.of("broken", "damaged", "wrong item", "not as described", "fake", "different"),
            "DUPLICATE_CHARGE", List.of("twice", "two times", "double", "charged again", "duplicate"),
            "CANCELLED_SUBSCRIPTION", List.of("cancelled", "canceled", "subscription", "after i cancel",
                    "unsubscribed"));

    private static final Pattern AMOUNT = Pattern.compile("(?:€|eur|euro)\\s?(\\d{1,7}(?:[.,]\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Proposal propose(Request request) {
        String text = request.maskedStatement().toLowerCase(Locale.ROOT);
        if (text.contains("#outage")) {
            throw new UnavailableException("scripted outage", null);
        }
        String best = "UNKNOWN";
        int bestHits = 0;
        for (var reason : List.of("FRAUD", "NOT_RECEIVED", "NOT_AS_DESCRIBED", "DUPLICATE_CHARGE",
                "CANCELLED_SUBSCRIPTION")) {
            int hits = (int) CUES.get(reason).stream().filter(text::contains).count();
            if (hits > bestHits) {
                best = reason;
                bestHits = hits;
            }
        }
        double confidence = bestHits == 0 ? 0.30 : Math.min(0.95, 0.55 + 0.20 * bestHits);
        int inputTokens = Math.max(1, text.length() / 4);
        return new Proposal(best, confidence, mentionedAmount(request.maskedStatement()),
                new Usage(NAME, inputTokens, 12, 0));
    }

    static BigDecimal mentionedAmount(String statement) {
        Matcher matcher = AMOUNT.matcher(statement);
        return matcher.find() ? new BigDecimal(matcher.group(1).replace(',', '.')) : null;
    }
}

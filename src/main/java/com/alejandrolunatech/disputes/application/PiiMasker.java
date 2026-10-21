package com.alejandrolunatech.disputes.application;

import java.util.regex.Pattern;

/**
 * Masks card numbers, IBANs, email addresses and phone numbers before text is stored or sent to a
 * model. Regexes catch the common formats; a production system would add a dedicated detector.
 */
public final class PiiMasker {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern IBAN = Pattern.compile("\\b[A-Z]{2}\\d{2}(?: ?[A-Z0-9]{4}){2,7}(?: ?[A-Z0-9]{1,3})?\\b");
    private static final Pattern CARD = Pattern.compile("\\b\\d(?:[ -]?\\d){12,18}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\w)\\+\\d{1,3}[ -]?(?:\\(0\\)[ -]?)?\\d(?:[ -]?\\d){7,11}\\b|\\b0\\d(?:[ -]?\\d){8}\\b");

    private PiiMasker() {
    }

    public static String mask(String text) {
        String masked = EMAIL.matcher(text).replaceAll("[EMAIL]");
        masked = IBAN.matcher(masked).replaceAll("[IBAN]");
        masked = CARD.matcher(masked).replaceAll("[CARD]");
        return PHONE.matcher(masked).replaceAll("[PHONE]");
    }
}

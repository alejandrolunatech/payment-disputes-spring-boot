package com.alejandrolunatech.disputes.domain;

/**
 * Why the customer disputes the charge. A simplified, made-up set for this project; real card
 * schemes have their own, longer lists.
 */
public enum ReasonCode {
    FRAUD,
    NOT_RECEIVED,
    NOT_AS_DESCRIBED,
    DUPLICATE_CHARGE,
    CANCELLED_SUBSCRIPTION
}
